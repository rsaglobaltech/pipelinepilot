package com.pipelinepilot.sandbox

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.pipelinepilot.api.LogEvent
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Project-scoped owner of the streaming log [ConsoleView]. The tool window mounts
 * this console; the sandbox poller writes [LogEvent]s into it (on the EDT).
 */
@Service(Service.Level.PROJECT)
class PipelinePilotLogConsole(project: Project) : Disposable {

    val console: ConsoleView by lazy {
        TextConsoleBuilderFactory.getInstance().createBuilder(project).console.also {
            Disposer.register(this, it)
        }
    }

    private val timeFmt = SimpleDateFormat("HH:mm:ss")

    fun clear() = console.clear()

    fun banner(text: String) =
        console.print("$text\n", ConsoleViewContentType.SYSTEM_OUTPUT)

    fun error(text: String) =
        console.print("$text\n", ConsoleViewContentType.ERROR_OUTPUT)

    fun print(event: LogEvent) {
        val ts = timeFmt.format(Date(event.timestampMillis))
        event.stage?.let {
            console.print("\n[$ts] === Stage: ${it.name} [${it.status}] ===\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        }
        event.line?.let {
            console.print("[$ts] $it\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
        event.buildStatus?.takeIf { event.finished }?.let {
            val type = if (it.equals("SUCCESS", true)) ConsoleViewContentType.SYSTEM_OUTPUT
            else ConsoleViewContentType.ERROR_OUTPUT
            console.print("\n[$ts] >>> Build finished: $it\n", type)
        }
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): PipelinePilotLogConsole = project.service()
    }
}
