package io.pocketbase.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncQueueTest {

    @Test
    fun testConcurrentEnqueue() = runTest {
        val count = 100
        val executedIndices = mutableSetOf<Int>()
        val testMutex = Mutex()
        val completer = CompletableDeferred<Unit>()

        val queue = SyncQueue(onComplete = {
            // Need to check count in a way that handles the fact that onComplete
            // might be called multiple times if the queue becomes empty intermittently.
            launch {
                testMutex.withLock {
                    if (executedIndices.size == count) {
                        completer.complete(Unit)
                    }
                }
            }
        })

        repeat(count) { i ->
            launch(Dispatchers.Default) {
                queue.enqueue {
                    delay(1)
                    testMutex.withLock {
                        executedIndices.add(i)
                    }
                }
            }
        }

        withTimeout(30000) {
            completer.await()
        }

        assertEquals(count, executedIndices.size)
    }
}
