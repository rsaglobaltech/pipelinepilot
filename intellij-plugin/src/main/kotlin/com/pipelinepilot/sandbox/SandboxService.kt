package com.pipelinepilot.sandbox

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.pipelinepilot.api.ReplayRequest
import com.pipelinepilot.api.RunSandboxRequest
import com.pipelinepilot.api.RunSandboxResponse
import com.pipelinepilot.api.SandboxClient
import com.pipelinepilot.api.SandboxStrategy
import com.pipelinepilot.settings.PipelinePilotSettings
import java.util.concurrent.CopyOnWriteArrayList

enum class SessionState { STARTING, RUNNING, FINISHED, FAILED, STOPPED }

data class SandboxSession(
    val sessionId: String,
    @Volatile var state: SessionState = SessionState.STARTING,
    val startedAt: Long = System.currentTimeMillis(),
)

fun interface SessionsListener {
    fun onChange(sessions: List<SandboxSession>)
}

/**
 * Orchestrates remote sandbox runs: create session, stream logs into the console,
 * then destroy the temporary execution. No local emulation — the run executes on
 * the real Jenkins instance.
 */
@Service(Service.Level.PROJECT)
class SandboxService(private val project: Project) : com.intellij.openapi.Disposable {

    private val client = SandboxClient()
    private val maxRetainedSessions = 10
    private val settings get() = PipelinePilotSettings.getInstance()
    private val log get() = PipelinePilotLogConsole.getInstance(project)
    private val stages get() = StageGraphModel.getInstance(project)

    private val sessions = CopyOnWriteArrayList<SandboxSession>()
    private val listeners = CopyOnWriteArrayList<SessionsListener>()

    // Last run inputs, used by replay.
    @Volatile private var lastJenkinsfile: String? = null
    @Volatile private var lastPipelineId: String? = null
    @Volatile private var lastBranch: String? = null
    @Volatile private var lastSessionId: String? = null

    fun sessions(): List<SandboxSession> = sessions.toList()

    fun addListener(l: SessionsListener) {
        listeners.add(l)
        l.onChange(sessions())
    }

    private fun fireChange() {
        val snapshot = sessions()
        listeners.forEach { it.onChange(snapshot) }
    }

    /** Start a sandbox run for [jenkinsfile] and stream its logs. Returns immediately. */
    fun run(jenkinsfile: String, pipelineId: String? = null, branch: String? = null) {
        lastJenkinsfile = jenkinsfile
        lastPipelineId = pipelineId
        lastBranch = branch
        val strategy = if (pipelineId != null) SandboxStrategy.REPLAY else SandboxStrategy.TEMP_FOLDER
        start("Running pipeline in sandbox", "started ($strategy)") {
            client.runSandbox(RunSandboxRequest(jenkinsfile, strategy, pipelineId, branch))
        }
    }

    /** Replay the whole last-run pipeline. */
    fun replayPipeline() = replay(stageName = null, onlyFailed = false)

    /** Replay only the stages that failed in the last run. */
    fun replayFailed() = replay(stageName = null, onlyFailed = true)

    /** Replay a single stage by name. */
    fun replayStage(stageName: String) = replay(stageName = stageName, onlyFailed = false)

    private fun replay(stageName: String?, onlyFailed: Boolean) {
        val jenkinsfile = lastJenkinsfile
        if (jenkinsfile == null) {
            notify("No previous run to replay — run a pipeline first", NotificationType.WARNING)
            return
        }
        val label = when {
            stageName != null -> "Replaying stage \"$stageName\""
            onlyFailed -> "Replaying failed stages"
            else -> "Replaying pipeline"
        }
        start(label, label) {
            client.replay(
                ReplayRequest(
                    jenkinsfile = jenkinsfile,
                    sourceSessionId = lastSessionId,
                    pipelineId = lastPipelineId,
                    branch = lastBranch,
                    stageName = stageName,
                    onlyFailedStages = onlyFailed,
                )
            )
        }
    }

    /** Shared lifecycle: create session via [producer], stream logs, then clean up. */
    private fun start(taskTitle: String, banner: String, producer: () -> RunSandboxResponse) {
        if (!settings.isConfigured()) {
            onEdt { log.error("PipelinePilot: Jenkins URL not configured (Settings | Tools | PipelinePilot)") }
            notify("Jenkins URL not configured", NotificationType.WARNING)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, taskTitle, true) {
            override fun run(indicator: ProgressIndicator) {
                val response = runCatching { producer() }.getOrElse { e ->
                    onEdt { log.error("PipelinePilot: $taskTitle failed — ${e.message}") }
                    notify("$taskTitle failed: ${e.message}", NotificationType.ERROR)
                    return
                }
                if (!response.accepted) {
                    onEdt { log.error("PipelinePilot: rejected — ${response.message ?: "no reason"}") }
                    return
                }

                lastSessionId = response.sessionId
                val session = SandboxSession(response.sessionId, SessionState.RUNNING)
                sessions.add(0, session)
                fireChange()
                onEdt {
                    log.clear()
                    stages.reset()
                    log.banner("=== Sandbox session ${session.sessionId} $banner ===")
                }

                streamLogs(session, indicator)
            }
        })
    }

    private fun streamLogs(session: SandboxSession, indicator: ProgressIndicator) {
        var start = 0L
        try {
            while (!indicator.isCanceled) {
                val chunk = runCatching { client.fetchLogs(session.sessionId, start) }.getOrElse { e ->
                    onEdt { log.error("PipelinePilot: log fetch error — ${e.message}") }
                    session.state = SessionState.FAILED
                    fireChange()
                    return
                }
                if (chunk.events.isNotEmpty()) onEdt {
                    chunk.events.forEach { event ->
                        log.print(event)
                        event.stage?.let { stages.update(it) }
                    }
                }
                start = chunk.nextStart
                if (!chunk.more) {
                    session.state = SessionState.FINISHED
                    fireChange()
                    break
                }
                Thread.sleep(settings.state.logPollIntervalMillis.toLong())
            }
            if (indicator.isCanceled) {
                session.state = SessionState.STOPPED
                fireChange()
                onEdt { log.banner("=== Streaming canceled ===") }
                client.deleteSession(session.sessionId)
                sessions.remove(session)
                fireChange()
            }
        } catch (t: Throwable) {
            client.deleteSession(session.sessionId)
            throw t
        }
        // NOTE: finished sessions are intentionally kept (job not deleted) so they can be
        // replayed per-stage. They are cleaned up on project close, on user "Stop & Clean
        // Up", or when older than the retention cap.
        trimOldSessions()
    }

    /** Keep Jenkins tidy: destroy the oldest finished sessions beyond the retention cap. */
    private fun trimOldSessions() {
        val finished = sessions.filter { it.state == SessionState.FINISHED || it.state == SessionState.FAILED }
        if (finished.size <= maxRetainedSessions) return
        finished.drop(maxRetainedSessions).forEach {
            client.deleteSession(it.sessionId)
            sessions.remove(it)
        }
        fireChange()
    }

    override fun dispose() {
        sessions.forEach { client.deleteSession(it.sessionId) }
        sessions.clear()
    }

    /** Cancel + cleanup a session that the user stops from the Sessions panel. */
    fun stop(session: SandboxSession) {
        session.state = SessionState.STOPPED
        fireChange()
        client.deleteSession(session.sessionId)
    }

    private fun onEdt(block: () -> Unit) =
        ApplicationManager.getApplication().invokeLater(block)

    private fun notify(text: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("PipelinePilot")
            .createNotification(text, type).notify(project)
    }

    companion object {
        fun getInstance(project: Project): SandboxService = project.service()
    }
}
