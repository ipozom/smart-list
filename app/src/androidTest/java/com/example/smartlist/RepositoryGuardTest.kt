package com.example.smartlist

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.BeforeClass
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*

class RepositoryGuardTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun setupInstrumentationInit() {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            ServiceLocator.initForInstrumentation(appContext)
        }
    }

    @Test
    fun addBlankItem_isIgnored_and_nonBlankIsPersisted() = runBlocking {
        val repo = ServiceLocator.provideRepository()

        // create a list
        repo.createList("RepoGuardList")
        val lists = repo.observeLists().first()
        assertTrue("Expected at least one list", lists.isNotEmpty())
        val listId = lists[0].id

        // Attempt to add a blank item (only whitespace)
        repo.addItem(listId, "   ")
        val itemsAfterBlank = repo.observeItems(listId).first()
        assertTrue("Blank item should be ignored", itemsAfterBlank.isEmpty())

        // Add a non-blank item and assert it's persisted
        repo.addItem(listId, "hello")
        val itemsAfterOk = repo.observeItems(listId).first()
        assertTrue("Expected at least one persisted item", itemsAfterOk.isNotEmpty())
        assertEquals("hello", itemsAfterOk[0].text)
    }
}
