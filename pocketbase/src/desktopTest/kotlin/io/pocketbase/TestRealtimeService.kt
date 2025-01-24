package io.pocketbase

import io.pocketbase.dtos.RecordSubscriptionEvent
import io.pocketbase.models.Demo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestRealtimeService : TestService() {
    private val collection by lazy {
        pb.collection<Demo>("demo")
    }

    @Test
    fun test_0_subscribeWithFilter_create() =
        runTest {
            authWithAdmin()

            val deferred = CompletableDeferred<RecordSubscriptionEvent<Demo>>()
            val unsubscribe =
                collection.subscribe(
                    topic = "*",
                    filter =
                        pb.filter(
                            expr = "title = {:title}",
                            query = mapOf("title" to "Filter subscription"),
                        ),
                    callback = { event ->
                        deferred.complete(event)
                    },
                )

            val record = collection.create(Demo(title = "Filter subscription"))

            val result = deferred.await()
            assertEquals(RecordSubscriptionEvent.Action.CREATE, result.action)
            assertEquals(record.id, result.record?.id)

            unsubscribe()
        }

    @Test
    fun test_1_subscribeWithFilter_update() =
        runTest {
            authWithAdmin()

            val deferred = CompletableDeferred<RecordSubscriptionEvent<Demo>>()
            val unsubscribe =
                collection.subscribe(
                    topic = "*",
                    filter =
                        pb.filter(
                            expr = "title = {:title}",
                            query = mapOf("title" to "Filter subscription updated"),
                        ),
                    callback = { event ->
                        deferred.complete(event)
                    },
                )

            val recordByTitle =
                collection.getFirstListItem(
                    filter =
                        pb.filter(
                            expr = "title = {:title}",
                            query = mapOf("title" to "Filter subscription"),
                        ),
                )

            assertNotNull(recordByTitle)

            val record =
                collection.update(
                    id = recordByTitle.id!!,
                    body = Demo(title = "Filter subscription updated"),
                )

            assertEquals("Filter subscription updated", record.title)

            val result = deferred.await()
            assertEquals(RecordSubscriptionEvent.Action.UPDATE, result.action)
            assertEquals(record.id, result.record?.id)

            unsubscribe()
        }

    @Test
    fun test_2_subscribeById_delete() =
        runTest {
            authWithAdmin()

            val recordByTitle =
                collection.getFirstListItem(
                    filter =
                        pb.filter(
                            expr = "title = {:title}",
                            query = mapOf("title" to "Filter subscription updated"),
                        ),
                )

            assertNotNull(recordByTitle)

            val deferred = CompletableDeferred<RecordSubscriptionEvent<Demo>>()
            val unsubscribe =
                collection.subscribe(
                    topic = recordByTitle.id!!,
                    callback = { event ->
                        deferred.complete(event)
                    },
                )

            collection.delete(recordByTitle.id!!)

            val result = deferred.await()
            assertEquals(RecordSubscriptionEvent.Action.DELETE, result.action)
            assertEquals(recordByTitle.id, result.record?.id)

            unsubscribe()
        }
}
