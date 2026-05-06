// NoteDialog.kt
package com.jetbrains.kmpapp.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NoteDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Float?) -> Unit,
    initialRating: Float? = null,
    initialText: String = ""
) {
    if (!isOpen) return

    var ratingText by remember { mutableStateOf(initialRating?.toString() ?: "") }
    var text by remember { mutableStateOf(initialText) }
    var ratingError by remember { mutableStateOf<String?>(null) }

    // FIXED: Removed 'private' keyword
    val validRatings = listOf(0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f)

    // Validate rating - must be exactly one of the allowed values
    fun validateRating(input: String): Float? {
        if (input.isBlank()) return null

        return try {
            val value = input.toFloat()
            if (value in validRatings) {
                ratingError = null
                value
            } else {
                ratingError = "Rating must be in half-point steps from 0.5 to 5"
                null
            }
        } catch (e: NumberFormatException) {
            ratingError = "Please enter a valid number"
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review album") },
        text = {
            Column {
                // Rating Field (First)
                OutlinedTextField(
                    value = ratingText,
                    onValueChange = {
                        ratingText = it
                        validateRating(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rating") },
                    placeholder = { Text("0.5, 1, 1.5, ..., 5") },
                    isError = ratingError != null,
                    supportingText = {
                        if (ratingError != null) {
                            Text(ratingError!!, color = Color.Red)
                        } else {
                            Text("Examples: 0.5, 1, 1.5, 2, … up to 5")
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Note Field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Review") },
                    placeholder = { Text("Enter your review here...") },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validatedRating = validateRating(ratingText)
                    if (validatedRating != null) {
                        onSave(text, validatedRating)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}