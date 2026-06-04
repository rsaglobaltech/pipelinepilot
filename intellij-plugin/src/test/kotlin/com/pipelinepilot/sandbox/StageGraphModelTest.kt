package com.pipelinepilot.sandbox

import com.pipelinepilot.api.StageInfo
import com.pipelinepilot.api.StageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StageGraphModelTest {

    @Test
    fun `top-level stages added in order and status updated`() {
        val m = StageGraphModel()
        m.update(StageInfo("Build", StageStatus.RUNNING))
        m.update(StageInfo("Test", StageStatus.PENDING))
        m.update(StageInfo("Build", StageStatus.SUCCESS)) // same stage updates in place

        val roots = m.roots()
        assertEquals(listOf("Build", "Test"), roots.map { it.name })
        assertEquals(StageStatus.SUCCESS, roots.first { it.name == "Build" }.status)
    }

    @Test
    fun `nested stage attaches under parent`() {
        val m = StageGraphModel()
        m.update(StageInfo("Parallel", StageStatus.RUNNING))
        m.update(StageInfo("unit", StageStatus.RUNNING, parent = "Parallel", parallel = true))
        m.update(StageInfo("integration", StageStatus.RUNNING, parent = "Parallel", parallel = true))

        val roots = m.roots()
        assertEquals(1, roots.size)
        assertEquals(listOf("unit", "integration"), roots.first().children.map { it.name })
    }
}
