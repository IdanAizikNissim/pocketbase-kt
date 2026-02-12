package io.pocketbase

import io.ktor.utils.io.core.toByteArray
import io.pocketbase.dtos.File
import io.pocketbase.models.Demo
import io.pocketbase.models.Todo
import io.pocketbase.models.TodoList
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestBatchService : TestService() {
    @Test
    fun test_0_create() =
        runTest {
            authWithAdmin()

            val todoListCollection =
                pb.collection<TodoList>(
                    idOrName = "todo_lists",
                )

            val listNumberOne =
                todoListCollection.create(
                    body =
                        TodoList(
                            title = "list number 1",
                        ),
                )

            assertNotNull(listNumberOne.id)

            val batch = pb.createBatch()
            with(
                batch.collection<Todo>(
                    idOrName = "todos",
                ),
            ) {
                create(
                    body =
                        Todo(
                            listId = listNumberOne.id!!,
                            text = "todo 1",
                        ),
                )

                create(
                    body =
                        Todo(
                            listId = listNumberOne.id!!,
                            text = "todo 2",
                        ),
                )

                create(
                    body =
                        Todo(
                            listId = listNumberOne.id!!,
                            text = "todo 3",
                        ),
                )
            }

            val result = batch.send()
            assertEquals(3, result.size)
            assertEquals(listOf("todo 1", "todo 2", "todo 3"), result.mapNotNull { (it.body as? Todo)?.text })
        }

    @Test
    fun test_1_deleteAll() =
        runTest {
            authWithAdmin()

            val todoListCollection =
                pb.collection<TodoList>(
                    idOrName = "todo_lists",
                )

            // Create a list for deletion test
            val list =
                todoListCollection.create(
                    body =
                        TodoList(
                            title = "list for delete",
                        ),
                )

            assertNotNull(list.id)

            // Create some todos to delete
            val todosCollection =
                pb.collection<Todo>(
                    idOrName = "todos",
                )

            todosCollection.create(Todo(listId = list.id!!, text = "delete me 1"))
            todosCollection.create(Todo(listId = list.id!!, text = "delete me 2"))

            val todos =
                todosCollection.getList(
                    filter =
                        pb.filter(
                            expr = "list_id = {:list_id}",
                            query = mapOf("list_id" to list.id!!),
                        ),
                )

            val batch = pb.createBatch()
            with(
                batch.collection<Todo>(
                    idOrName = "todos",
                ),
            ) {
                todos.items.forEach {
                    delete(it.id!!)
                }
            }

            val result = batch.send()
            assertEquals(2, result.size)

            todoListCollection.delete(list.id!!)
        }

    @Test
    fun test_2_createWithFile() =
        runTest {
            authWithAdmin()

            val batch = pb.createBatch()
            with(batch.collection<Demo>("demo")) {
                create(
                    body = Demo(title = "test_file_upload"),
                    files = listOf(
                        File(
                            field = "file",
                            fileName = "test.txt",
                            data = "content".toByteArray()
                        )
                    )
                )
            }

            val result = batch.send()
            assertEquals(1, result.size)
            val demo = result.first().body as Demo
            assertEquals("test_file_upload", demo.title)

            // If file upload worked, the file name should be present
            // Note: This relies on 'file' being the correct field name in the collection
            // If it fails because file is null, check schema. But if it fails, it proves current BatchService is broken.
            assertNotNull(demo.file)
            assertTrue(demo.file!!.isNotEmpty())

            // Clean up
            pb.collection<Demo>("demo").delete(demo.id!!)
        }

    @Test
    fun test_3_upsertAndUpdate() =
        runTest {
            authWithAdmin()

            val batch = pb.createBatch()
            with(batch.collection<Demo>("demo")) {
                upsert(
                    body = Demo(title = "upsert_title"),
                )
            }
            val upsertResult = batch.send()
            assertEquals(1, upsertResult.size)
            val upserted = upsertResult.first().body as Demo
            assertNotNull(upserted.id)

            val batch2 = pb.createBatch()
            with(batch2.collection<Demo>("demo")) {
                update(
                    recordId = upserted.id!!,
                    body = Demo(title = "upsert_title_updated"),
                )
            }
            val updateResult = batch2.send()
            assertEquals(1, updateResult.size)
            val updated = updateResult.first().body as Demo
            assertEquals("upsert_title_updated", updated.title)

            pb.collection<Demo>("demo").delete(updated.id!!)
        }
}
