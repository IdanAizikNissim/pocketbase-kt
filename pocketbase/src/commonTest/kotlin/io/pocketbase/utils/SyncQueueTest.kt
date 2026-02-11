package io.pocketbase.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncQueueTest {

    @Test
    fun testConcurrentEnqueue() = runTest {
        val count = 1000
        var executedCount = 0
        val completer = CompletableDeferred<Unit>()

        val queue = SyncQueue(onComplete = {
            if (executedCount == count) {
                completer.complete(Unit)
            }
        })

        repeat(count) {
            launch(Dispatchers.Default) {
                queue.enqueue {
                    delay(1)
                    executedCount++
                }
            }
        }

        withTimeout(10000) {
            completer.await()
        }

        assertEquals(count, executedCount)
    }
}
