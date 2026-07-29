package com.kemi.desklink.workspace

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceCoordinatorTest {
    @AfterTest
    fun reset() {
        WorkspaceCoordinator.resetForTest()
    }

    @Test
    fun updateKeepsOneAuthoritativeSession() {
        val updated = WorkspaceCoordinator.update {
            it.copy(draftText = "KEMI 语音提交", selectionVersion = it.selectionVersion + 1)
        }

        assertEquals("KEMI 语音提交", updated.draftText)
        assertEquals(updated, WorkspaceCoordinator.snapshot())
        assertEquals(1L, updated.selectionVersion)
    }
}

