package io.pocketbase.sample.viewmodel

import io.github.vinceglb.filekit.core.PlatformFile
import io.pocketbase.sample.client.PocketBaseSingleton
import io.pocketbase.sample.data.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoViewModel(private val scope: CoroutineScope) {
    private val client = PocketBaseSingleton.client
    private val todoCollection = client.collection("todos", Todo::class)

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos = _todos.asStateFlow()

    init {
        // Initial fetch
        fetchTodos()

        // Realtime subscription
        scope.launch(Dispatchers.IO) {
            try {
                // We must accept the event argument, even if we ignore it
                todoCollection.subscribe("*", { _ ->
                    // Refresh list on any change
                    fetchTodos()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchTodos() {
        scope.launch(Dispatchers.IO) {
            try {
                // sort by -created to see newest first
                val result = todoCollection.getList(1, 50, sort = "-created")
                _todos.value = result.items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createTodo(text: String, file: PlatformFile? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val todo = Todo(text = text, completed = false)
                if (file != null) {
                    val bytes = file.readBytes()
                    val pbFile = io.pocketbase.dtos.File(
                        field = "attachment",
                        fileName = file.name,
                        data = bytes
                    )
                    todoCollection.create(todo, files = listOf(pbFile))
                } else {
                    todoCollection.create(todo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleTodo(todo: Todo) {
        scope.launch(Dispatchers.IO) {
            val id = todo.id ?: return@launch
            try {
                val updated = Todo(
                    text = todo.text,
                    completed = !todo.completed,
                    attachment = todo.attachment
                )
                todoCollection.update(id, updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        scope.launch(Dispatchers.IO) {
            val id = todo.id ?: return@launch
            try {
                todoCollection.delete(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
