package com.offline.filemanager.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.filemanager.ActiveDialog
import com.offline.filemanager.FileItem
import com.offline.filemanager.LocalFileRules

@Composable
fun FileActionDialogHost(
    activeDialog: ActiveDialog?,
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onRename: (FileItem, String) -> Unit,
    onDelete: (List<FileItem>, Boolean) -> Unit,
    onZip: (String) -> Unit
) {
    if (activeDialog == null) return

    when (activeDialog) {
        is ActiveDialog.CreateFolder -> {
            var folderName by remember { mutableStateOf("") }
            var errorText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("New Folder", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = {
                                folderName = it
                                val (valid, err) = LocalFileRules.validateName(it)
                                errorText = if (!valid && it.isNotEmpty()) err else ""
                            },
                            label = { Text("Folder Name") },
                            isError = errorText.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_folder_input")
                        )
                        if (errorText.isNotEmpty()) {
                            Text(text = errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onCreateFolder(folderName) },
                        enabled = folderName.isNotBlank() && errorText.isEmpty(),
                        modifier = Modifier.testTag("create_folder_confirm")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is ActiveDialog.CreateFile -> {
            var fileName by remember { mutableStateOf("") }
            var errorText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("New File", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = {
                                fileName = it
                                val (valid, err) = LocalFileRules.validateName(it)
                                errorText = if (!valid && it.isNotEmpty()) err else ""
                            },
                            label = { Text("File Name (e.g. document.txt)") },
                            isError = errorText.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_file_input")
                        )
                        if (errorText.isNotEmpty()) {
                            Text(text = errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onCreateFile(fileName) },
                        enabled = fileName.isNotBlank() && errorText.isEmpty(),
                        modifier = Modifier.testTag("create_file_confirm")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is ActiveDialog.Rename -> {
            var newName by remember { mutableStateOf(activeDialog.item.name) }
            var errorText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Rename", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                val (valid, err) = LocalFileRules.validateName(it)
                                errorText = if (!valid && it.isNotEmpty()) err else ""
                            },
                            label = { Text("New Name") },
                            isError = errorText.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_input")
                        )
                        if (errorText.isNotEmpty()) {
                            Text(text = errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onRename(activeDialog.item, newName) },
                        enabled = newName.isNotBlank() && errorText.isEmpty() && newName != activeDialog.item.name,
                        modifier = Modifier.testTag("rename_confirm")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is ActiveDialog.DeleteConfirm -> {
            val count = activeDialog.items.size
            val isPermanent = activeDialog.permanent

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(if (isPermanent) "Delete Permanently?" else "Move to Trash?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (isPermanent) {
                            "Are you sure you want to permanently delete $count selected item(s)? This action cannot be undone."
                        } else {
                            "Are you sure you want to move $count selected item(s) to Trash? You can restore them later."
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onDelete(activeDialog.items, isPermanent) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_confirm_btn")
                    ) {
                        Text(if (isPermanent) "Delete Permanently" else "Move to Trash")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is ActiveDialog.FileDetails -> {
            val item = activeDialog.item
            val context = LocalContext.current

            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("File Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow("Name", item.name)
                        DetailRow("Type", if (item.isDirectory) "Folder" else item.mimeType)
                        DetailRow("Path", item.path)
                        DetailRow("Size", if (item.isDirectory) "${item.itemCount} items (${LocalFileRules.formatFileSize(item.size)})" else LocalFileRules.formatFileSize(item.size))
                        DetailRow("Last Modified", LocalFileRules.formatDate(item.lastModified))

                        if (activeDialog.checksum.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SHA-256 Checksum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeDialog.checksum,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Checksum", activeDialog.checksum))
                                    Toast.makeText(context, "Checksum copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Checksum", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) { Text("Close") }
                }
            )
        }

        is ActiveDialog.ZipItems -> {
            var zipName by remember { mutableStateOf(activeDialog.defaultName) }
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Compress to ZIP", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = zipName,
                        onValueChange = { zipName = it },
                        label = { Text("Archive Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zip_name_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onZip(zipName) },
                        enabled = zipName.isNotBlank(),
                        modifier = Modifier.testTag("zip_confirm_btn")
                    ) {
                        Text("Compress")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
