package com.example.dsaadmin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun saveNote(userId: String, questionId: String, note: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .document(userId)
        .collection("notes")
        .document(questionId)
        .set(mapOf("note" to note))
}

fun fetchNote(userId: String, questionId: String, onResult: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users")
        .document(userId)
        .collection("notes")
        .document(questionId)
        .get()
        .addOnSuccessListener { document ->
            val note = document.getString("note") ?: ""
            onResult(note)
        }
        .addOnFailureListener {
            onResult("")
        }
}



@Composable
fun NoteDialog(
    userId: String,
    questionId: String,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(questionId) {
        fetchNote(userId, questionId) {
            noteText = it
            isLoading = false
        }
    }

    if (!isLoading) {
        AlertDialog(
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = onDismiss,
            title = { Text("Add/Edit Note")},
            text = {
                Column {

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Your thoughts on this question...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    saveNote(userId, questionId, noteText)
                    onDismiss()
                }) {
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
}




