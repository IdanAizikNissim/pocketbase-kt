package io.pocketbase.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class SyncQueueTest {
    @Test
    fun testSequentialExecution() =
        runTest {
            val result = Collections.synchronizedList(mutableListOf<String>())
            val completion = CompletableDeferred<Unit>()

            val queue =
                SyncQueue(onComplete = {
                    completion.complete(Unit)
                })

            queue.enqueue {
                delay(50)
                result.add("1")
            }

            queue.enqueue {
                delay(10)
                result.add("2")
            }

            queue.enqueue {
                result.add("3")
            }

            withContext(Dispatchers.IO) {
                completion.await()
            }

            assertEquals(listOf("1", "2", "3"), result)
        }

    @Test
    fun testPerformance() =
        runTest {
            val iterations = 5000 // Increased back to 5000 to show improvement
            val counter = java.util.concurrent.atomic.AtomicInteger(0)
            val latch = java.util.concurrent.CountDownLatch(iterations)

            val queue = SyncQueue()

            val time =
                measureTime {
                    repeat(iterations) {
                        queue.enqueue {
                            counter.incrementAndGet()
                            latch.countDown()
                        }
                    }
                    withContext(Dispatchers.IO) {
                        latch.await()
                    }
                }

            assertEquals(iterations, counter.get())
            println("Performance test completed in $time for $iterations items")
        }
}
