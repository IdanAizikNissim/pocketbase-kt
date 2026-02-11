package io.pocketbase.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import io.pocketbase.sample.data.Todo
import io.pocketbase.sample.viewmodel.AuthViewModel
import io.pocketbase.sample.viewmodel.TodoViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel,
    onLogout: () -> Unit,
    onTodoClick: (Todo) -> Unit
) {
    val todos by todoViewModel.todos.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(todos) { todo ->
                TodoItem(
                    todo = todo,
                    onToggle = { todoViewModel.toggleTodo(todo) },
                    onDelete = { todo.id?.let { todoViewModel.deleteTodo(it) } },
                    onClick = { onTodoClick(todo) }
                )
            }
        }

        if (showAddDialog) {
            AddTodoDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { text, file ->
                    todoViewModel.createTodo(text, file)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(todo.text, style = MaterialTheme.typography.body1)
                if (todo.attachment?.isNotEmpty() == true) {
                    Text("Attachment: ${todo.attachment}", style = MaterialTheme.typography.caption)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun AddTodoDialog(onDismiss: () -> Unit, onAdd: (String, PlatformFile?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var file by remember { mutableStateOf<PlatformFile?>(null) }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(),
        mode = PickerMode.Single,
        title = "Select attachment"
    ) { file = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task") }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch() }) {
                    Text(if (file == null) "Attach File" else "File: ${file!!.name}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                println("DEBUG: Add button clicked, text='$text', file=${file?.name}")
                onAdd(text, file)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
