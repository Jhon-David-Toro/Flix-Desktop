package com.flix.music.core.system

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SingleInstanceTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `una segunda apertura no arranca otra Flix y pide mostrar la ventana`() {
        val first = SingleInstance.acquire(folder.root)
        assertNotNull("la primera debería arrancar", first)
        val showRequested = CountDownLatch(1)
        first!!.listen { showRequested.countDown() }

        val second = SingleInstance.acquire(folder.root)

        assertNull(second)
        assertTrue(showRequested.await(5, TimeUnit.SECONDS))
        first.release()
    }
}
