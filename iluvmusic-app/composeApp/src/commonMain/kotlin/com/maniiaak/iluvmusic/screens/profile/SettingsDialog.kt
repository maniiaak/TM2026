package com.maniiaak.iluvmusic.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(currentImageUrl: String?, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var imageUrl by remember(currentImageUrl) { mutableStateOf(currentImageUrl.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile settings") },
        text = {
            Column {
                Text("Add a profile picture using an image URL.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Profile picture URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // NOTE: add a matching "Banner image URL" field here once banner uploads
                // are wired up on the viewmodel/backend side.
            }
        },
        confirmButton = { TextButton(onClick = { onSave(imageUrl.trim().ifBlank { null }) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (!currentImageUrl.isNullOrBlank()) TextButton(onClick = { onSave(null) }) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}