package io.pocketbase.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import io.pocketbase.sample.client.PocketBaseSingleton
import io.pocketbase.sample.data.Todo
import io.pocketbase.sample.viewmodel.TodoViewModel

@Composable
fun TodoDetailScreen(
    todoId: String,
    viewModel: TodoViewModel,
    onBack: () -> Unit
) {
    fun isImageFile(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp")
    }
    // In a real app, ViewModel would fetch this on init and expose state
    // Here we manage local state for simplicity
    var text by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var existingAttachment by remember { mutableStateOf<String?>(null) }
    var existingAttachmentRecordId by remember { mutableStateOf<String?>(null) }
    var newFile by remember { mutableStateOf<PlatformFile?>(null) }
    var deleteAttachment by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var fetchedTodo by remember { mutableStateOf<Todo?>(null) }

    LaunchedEffect(todoId) {
        viewModel.getTodo(todoId) { fetched ->
            if (fetched != null) {
                fetchedTodo = fetched
                text = fetched.text
                completed = fetched.completed
                user = fetched.user
                existingAttachment = fetched.attachment.takeIf { it?.isNotEmpty() == true }
                existingAttachmentRecordId = fetched.id
            }
            isLoading = false
        }
    }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(),
        mode = PickerMode.Single,
        title = "Select attachment"
    ) { file ->
        newFile = file
        if (file != null) {
            deleteAttachment = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Todo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = completed, onCheckedChange = { completed = it })
                    Text("Completed")
                }
                Spacer(Modifier.height(16.dp))

                Text("Attachment:", style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(8.dp))

                if (existingAttachment != null && !deleteAttachment) {
                    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            if (isImageFile(existingAttachment!!)) {
                                val imageUrl = remember(existingAttachment, existingAttachmentRecordId) {
                                    existingAttachment?.let {
                                        fetchedTodo?.let { todo ->
                                            PocketBaseSingleton.client.files.getUrl(record = todo, existingAttachment!!)
                                        }
                                    } ?: existingAttachment
                                }
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Attachment",
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(if (isImageFile(existingAttachment!!)) "Image attached" else existingAttachment!!)
                                TextButton(onClick = { deleteAttachment = true }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                } else if (newFile != null) {
                     Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("New: ${newFile!!.name}")
                            TextButton(onClick = { newFile = null }) {
                                Text("Clear")
                            }
                        }
                    }
                } else {
                    Text("No attachment", style = MaterialTheme.typography.caption)
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch() }) {
                    Text(if (existingAttachment != null || newFile != null) "Change File" else "Attach File")
                }

                Spacer(Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteTodo(todoId)
                            onBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error)
                    ) {
                        Text("Delete")
                    }

                    Button(
                        onClick = {
                            viewModel.updateTodo(todoId, user, text, completed, newFile, deleteAttachment)
                            onBack()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
