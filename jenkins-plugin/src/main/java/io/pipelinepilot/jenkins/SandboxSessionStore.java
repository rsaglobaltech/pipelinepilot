package io.pipelinepilot.jenkins;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Descriptor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.RestartDeclarativePipelineAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Creates and tracks ephemeral sandbox executions. Each session is a throwaway
 * {@link WorkflowJob} created under a hidden folder ("IDE-SANDBOX/&lt;user&gt;"),
 * run with the Groovy sandbox enabled, then deleted on cleanup.
 *
 * No local emulation — execution happens on this real Jenkins instance, reusing
 * its configured agents, credentials, shared libraries and plugins.
 */
public final class SandboxSessionStore {

    private static final Logger LOG = Logger.getLogger(SandboxSessionStore.class.getName());
    private static final String ROOT_FOLDER = "IDE-SANDBOX";

    private static final SandboxSessionStore INSTANCE = new SandboxSessionStore();
    public static SandboxSessionStore get() { return INSTANCE; }

    private final Map<String, SandboxSession> sessions = new ConcurrentHashMap<>();

    private SandboxSessionStore() {}

    public SandboxSession find(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Create a temporary job for {@code jenkinsfile}, schedule it with the sandbox
     * enabled, and start tracking it. Returns the new session.
     */
    public SandboxSession createAndRun(String jenkinsfile, String user)
            throws IOException, Descriptor.FormException {
        Jenkins jenkins = Jenkins.get();
        String sessionId = UUID.randomUUID().toString();

        Folder userFolder = ensureFolder(jenkins, user);
        WorkflowJob job = userFolder.createProject(WorkflowJob.class, "session-" + sessionId);
        // sandbox = true: run untrusted IDE-submitted scripts under the Groovy sandbox.
        job.setDefinition(new CpsFlowDefinition(jenkinsfile, true));

        SandboxSession session = new SandboxSession(sessionId, user);
        session.setJob(job);
        sessions.put(sessionId, session);

        Future<WorkflowRun> future = job.scheduleBuild2(0,
                new CauseAction(new Cause.RemoteCause("IDE", "PipelinePilot sandbox")));
        if (future == null) {
            session.setState(SandboxSession.State.FAILED);
            throw new IOException("Failed to schedule sandbox build");
        }
        session.setState(SandboxSession.State.RUNNING);
        // Resolve the WorkflowRun without blocking the request thread for the whole build.
        new Thread(() -> {
            try {
                session.setRun(future.get());
            } catch (Exception e) {
                session.setState(SandboxSession.State.FAILED);
                LOG.warning("Sandbox run resolution failed: " + e.getMessage());
            }
        }, "pp-session-" + sessionId).start();

        return session;
    }

    /**
     * Restart a finished declarative run from a specific stage (true per-stage replay,
     * via {@link RestartDeclarativePipelineAction}). The new build runs on the same job;
     * a fresh session is returned to stream it. Throws if the source run is gone or the
     * stage is not restartable (e.g. scripted pipeline) — callers fall back to a full replay.
     */
    public SandboxSession restartStage(String sourceSessionId, String stageName) throws Exception {
        SandboxSession source = sessions.get(sourceSessionId);
        if (source == null || source.getRun() == null) {
            throw new IllegalStateException("Source run is no longer available — run the pipeline again");
        }
        WorkflowRun sourceRun = source.getRun();
        RestartDeclarativePipelineAction action = sourceRun.getAction(RestartDeclarativePipelineAction.class);
        if (action == null || !action.isRestartEnabled()) {
            throw new IllegalStateException("Run is not restartable (declarative, finished pipelines only)");
        }
        if (!action.getRestartableStages().contains(stageName)) {
            throw new IllegalStateException("Stage not restartable: " + stageName);
        }

        Queue.Item item = action.run(stageName);
        if (item == null) {
            throw new IOException("Failed to schedule stage restart");
        }

        String sessionId = UUID.randomUUID().toString();
        SandboxSession session = new SandboxSession(sessionId, source.getUser());
        session.setJob(source.getJob());
        session.setState(SandboxSession.State.RUNNING);
        sessions.put(sessionId, session);

        QueueTaskFuture<? extends Queue.Executable> future = item.getFuture();
        new Thread(() -> {
            try {
                Queue.Executable exec = future.get();
                if (exec instanceof WorkflowRun) {
                    session.setRun((WorkflowRun) exec);
                } else {
                    session.setState(SandboxSession.State.FAILED);
                }
            } catch (Exception e) {
                session.setState(SandboxSession.State.FAILED);
                LOG.warning("Stage restart run resolution failed: " + e.getMessage());
            }
        }, "pp-restart-" + sessionId).start();

        return session;
    }

    /** Destroy the temporary job/run for a session and stop tracking it. */
    public void destroy(String sessionId) {
        SandboxSession session = sessions.remove(sessionId);
        if (session == null) return;
        try {
            WorkflowRun run = session.getRun();
            if (run != null && run.isBuilding()) {
                run.doStop();
            }
            WorkflowJob job = session.getJob();
            if (job != null) {
                job.delete();
            }
            session.setState(SandboxSession.State.STOPPED);
        } catch (Exception e) {
            LOG.warning("Sandbox cleanup failed for " + sessionId + ": " + e.getMessage());
        }
    }

    /** Map a finished run's result to a session state. */
    public void refreshState(SandboxSession session) {
        WorkflowRun run = session.getRun();
        if (run == null) return;
        if (run.isBuilding()) {
            session.setState(SandboxSession.State.RUNNING);
        } else {
            Result r = run.getResult();
            session.setState(r != null && r.isBetterOrEqualTo(Result.SUCCESS)
                    ? SandboxSession.State.FINISHED : SandboxSession.State.FAILED);
        }
    }

    private Folder ensureFolder(Jenkins jenkins, String user) throws IOException, Descriptor.FormException {
        Folder root = (Folder) jenkins.getItem(ROOT_FOLDER);
        if (root == null) {
            root = jenkins.createProject(Folder.class, ROOT_FOLDER);
        }
        String safeUser = user.replaceAll("[^a-zA-Z0-9_-]", "_");
        Folder userFolder = (Folder) root.getItem(safeUser);
        if (userFolder == null) {
            userFolder = root.createProject(Folder.class, safeUser);
        }
        return userFolder;
    }
}
