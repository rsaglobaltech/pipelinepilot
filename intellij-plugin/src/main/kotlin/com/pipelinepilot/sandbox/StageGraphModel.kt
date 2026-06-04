package com.pipelinepilot.sandbox

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.pipelinepilot.api.StageInfo
import com.pipelinepilot.api.StageStatus
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Builds a stage tree from streamed [StageInfo] events. Top-level stages run in
 * sequence; children are nested; siblings flagged [Node.parallel] run in parallel.
 */
@Service(Service.Level.PROJECT)
class StageGraphModel {

    class Node(
        val name: String,
        @Volatile var status: StageStatus,
        val parent: Node?,
        @Volatile var parallel: Boolean,
        val order: Int,
    ) {
        val children = CopyOnWriteArrayList<Node>()
    }

    fun interface Listener { fun onChange(roots: List<Node>) }

    private val byKey = LinkedHashMap<String, Node>()
    private val roots = CopyOnWriteArrayList<Node>()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private var counter = 0

    @Synchronized
    fun reset() {
        byKey.clear()
        roots.clear()
        counter = 0
        fire()
    }

    fun addListener(l: Listener) {
        listeners.add(l)
        l.onChange(roots.toList())
    }

    @Synchronized
    fun update(stage: StageInfo) {
        val key = (stage.parent?.let { "$it/" } ?: "") + stage.name
        val existing = byKey[key]
        if (existing != null) {
            existing.status = stage.status
            existing.parallel = stage.parallel
        } else {
            val parentNode = stage.parent?.let { byKey[it] ?: byKey.values.firstOrNull { n -> n.name == stage.parent } }
            val node = Node(stage.name, stage.status, parentNode, stage.parallel, counter++)
            byKey[key] = node
            if (parentNode != null) parentNode.children.add(node) else roots.add(node)
        }
        fire()
    }

    fun roots(): List<Node> = roots.toList()

    private fun fire() {
        val snapshot = roots.toList()
        listeners.forEach { it.onChange(snapshot) }
    }

    companion object {
        fun getInstance(project: Project): StageGraphModel = project.service()
    }
}
