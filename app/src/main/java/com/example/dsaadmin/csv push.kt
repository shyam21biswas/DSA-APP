package com.example.dsaadmin

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.InputStreamReader


@Composable
fun AddQuestionFromCSVScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var companyList by remember { mutableStateOf(listOf<String>()) }
    var selectedCompany by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadCSVToFirebase(context, uri, selectedCompany, db) {
                message = it
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("companies").get().addOnSuccessListener { result ->
            companyList = result.documents.mapNotNull { it.id }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Upload CSV Questions", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuWithLabels(
            label = "Select Company",
            options = companyList,
            selectedOption = selectedCompany,
            onOptionSelected = { selectedCompany = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (selectedCompany.isNotBlank()) {
                launcher.launch("*/*")
            } else {
                message = "Please select a company first"
            }
        }) {
            Text("Select CSV File")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
    }
}

fun uploadCSVToFirebase(
    context: Context,
    uri: Uri,
    selectedCompany: String,
    db: FirebaseFirestore,
    onComplete: (String) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var count = 0

        val headers = reader.readLine()?.split(",") ?: return onComplete("❌ Invalid CSV header")

        val idIndex = headers.indexOf("ID")
        val titleIndex = headers.indexOf("Title")
        val difficultyIndex = headers.indexOf("Difficulty")
        val linkIndex = headers.indexOf("Leetcode Question Link")
        val tagsIndex = headers.indexOf("Tags")

        if (listOf(idIndex, titleIndex, difficultyIndex, linkIndex, tagsIndex).any { it == -1 }) {
            return onComplete("❌ One or more required columns not found in CSV.")
        }

        while (reader.readLine().also { line = it } != null) {
            val values = line?.split(",") ?: continue
            if (values.size <= tagsIndex) continue  // Ensure all required indices exist

            val leetnum = values.getOrNull(idIndex)?.trim() ?: continue
            val title = values.getOrNull(titleIndex)?.trim() ?: continue
            val difficulty = values.getOrNull(difficultyIndex)?.trim() ?: continue
            val link = values.getOrNull(linkIndex)?.trim() ?: continue
            val tagsRaw = values.getOrNull(tagsIndex)?.trim() ?: ""
            val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val questionData = hashMapOf(
                "leetnumber" to leetnum,
                "title" to title,
                "difficulty" to difficulty,
                "tags" to tags,
                "link" to link
                 // ✅ add this
            )

            db.collection("companies")
                .document(selectedCompany)
                .collection("questions")
                .add(questionData)
                .addOnSuccessListener {
                    count++
                    onComplete("✅ Uploaded $count questions so far.")
                }
        }

        reader.close()
        onComplete("✅ Finished uploading $count questions.")
    } catch (e: Exception) {
        onComplete("❌ Error: ${e.localizedMessage}")
    }
}



@Composable
fun DropdownMenuWithLabels(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label)
        Box {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                readOnly = true,
                label = { Text("Select") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }) {
                        Text(option)
                    }
                }
            }
        }
    }
}

