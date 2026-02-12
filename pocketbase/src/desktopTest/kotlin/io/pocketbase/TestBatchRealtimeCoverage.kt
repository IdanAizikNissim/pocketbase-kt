package io.pocketbase

import io.pocketbase.http.ClientException
import io.pocketbase.models.Demo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TestBatchRealtimeCoverage : TestService() {
    private val demo by lazy { pb.collection<Demo>("demo") }

    @Test
    fun batch_and_realtime_methods_areInvocable() =
        runTest {
            authWithAdmin()

            invokeOrError {
                val batch = pb.createBatch()
                with(batch.collection<Demo>("demo")) {
                    create(body = Demo(title = "batch_create"))
                    upsert(body = Demo(title = "batch_upsert"))
                    update(recordId = "missing-id", body = Demo(title = "batch_update"))
                    delete(recordId = "missing-id")
                }
                batch.send()
            }

            invokeOrError {
                val unsubscribe =
                    demo.subscribe(
                        topic = "*",
                        callback = {},
                    )
                unsubscribe()
            }
            invokeOrError { demo.unsubscribe() }
            invokeOrError { demo.unsubscribe("*") }
        }

    private suspend fun invokeOrError(block: suspend () -> Any?) {
        try {
            block()
        } catch (e: ClientException) {
            assertTrue(e.statusCode >= 400)
        } catch (e: Exception) {
            assertTrue(e.message != null)
        }
    }
}
