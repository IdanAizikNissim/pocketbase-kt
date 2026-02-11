package io.pocketbase.sample.viewmodel

import io.github.vinceglb.filekit.core.PlatformFile
import io.pocketbase.dtos.RecordModel
import io.pocketbase.sample.client.PocketBaseSingleton
import io.pocketbase.sample.data.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import io.pocketbase.dtos.File as PBFile

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
                todoCollection.subscribe(
                    topic = "*",
                    callback = { _ ->
                        // Refresh list on any change
                        fetchTodos()
                    }
                )
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

    fun getTodo(id: String, onResult: (Todo?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val todo = todoCollection.getOne(id)
                onResult(todo)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun createTodo(text: String, file: PlatformFile? = null) {
        println("DEBUG: createTodo called with text='$text', file=${file?.name}")
        scope.launch(Dispatchers.IO) {
            try {
                val userId = (client.authStore.model as? RecordModel)?.id ?: run {
                    println("DEBUG: userId is null, aborting")
                    return@launch
                }
                println("DEBUG: userId=$userId")

                val todo = Todo(
                    text = text,
                    completed = false,
                    user = userId
                )
                println("DEBUG: TodoCreate object created")

                val files = if (file != null) {
                    println("DEBUG: reading bytes for file: ${file.name}")
                    val bytes = file.readBytes()
                    println("DEBUG: file bytes read, size=${bytes.size}")
                    listOf(PBFile(
                        field = "attachment",
                        fileName = file.name,
                        data = bytes
                    ))
                } else {
                    println("DEBUG: no file to attach")
                    emptyList()
                }
                println("DEBUG: calling client.collection().create()")
                client.collection("todos", Todo::class).create(todo, files = files)
                println("DEBUG: createTodo completed successfully")

            } catch (e: Exception) {
                println("DEBUG: exception in createTodo: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateTodo(
        id: String,
        user: String,
        text: String,
        completed: Boolean,
        file: PlatformFile? = null,
        deleteAttachment: Boolean = false
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val attachmentVal = if (deleteAttachment) "" else null

                val updateObj = Todo(
                    text = text,
                    completed = completed,
                    attachment = attachmentVal,
                    user = user,
                )

                val files = if (file != null) {
                    val bytes = file.readBytes()
                    listOf(PBFile(
                        field = "attachment",
                        fileName = file.name,
                        data = bytes
                    ))
                } else emptyList()

                client.collection("todos", Todo::class).update(id, updateObj, files = files)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleTodo(todo: Todo) {
        scope.launch(Dispatchers.IO) {
            val id = todo.id ?: return@launch
            try {
                val updateObj = todo.copy(completed = !todo.completed)
                client.collection("todos", Todo::class).update(id, updateObj)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTodo(id: String) {
        scope.launch(Dispatchers.IO) {
            try {
                todoCollection.delete(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
