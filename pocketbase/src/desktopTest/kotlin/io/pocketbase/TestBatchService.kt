package io.pocketbase

import io.pocketbase.models.Todo
import io.pocketbase.models.TodoList
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

            val list =
                todoListCollection.getFirstListItem(
                    filter =
                        pb.filter(
                            expr = "title = {:title}",
                            query = mapOf("title" to "list number 1"),
                        ),
                )

            assertNotNull(list?.id)

            val todosCollection =
                pb.collection<Todo>(
                    idOrName = "todos",
                )
            val todos =
                todosCollection.getList(
                    filter =
                        pb.filter(
                            expr = "list_id = {:list_id}",
                            query = mapOf("list_id" to list!!.id!!),
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
            assertEquals(3, result.size)

            todoListCollection.delete(list.id!!)
        }
}
