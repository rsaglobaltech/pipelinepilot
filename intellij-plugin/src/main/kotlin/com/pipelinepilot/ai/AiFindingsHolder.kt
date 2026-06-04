package com.pipelinepilot.ai

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores the most recent AI review findings per file so the annotator can render them
 * as inline gutter markers + tooltips. A review snapshot; cleared on re-review or fix.
 */
@Service(Service.Level.PROJECT)
class AiFindingsHolder {

    private val byPath = ConcurrentHashMap<String, List<AiFinding>>()

    fun set(path: String, findings: List<AiFinding>) {
        if (findings.isEmpty()) byPath.remove(path) else byPath[path] = findings
    }

    fun get(path: String): List<AiFinding> = byPath[path] ?: emptyList()

    fun clear(path: String) {
        byPath.remove(path)
    }

    companion object {
        fun getInstance(project: Project): AiFindingsHolder = project.service()
    }
}
