package com.example.dsaadmin

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore




@Composable
fun AdminHomeScreen() {
    var screen by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { screen = "add_company" }, modifier = Modifier.fillMaxWidth()) {
            Text("Add New Company")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { screen = "add_question" }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Question to Existing Company")
        }
        Spacer(modifier = Modifier.height(32.dp))

        when (screen) {
            "add_company" -> AddCompanyScreen()
            "add_question" -> AddQuestionScreen()
        }
    }
}

@Composable
fun AddCompanyScreen() {
    val db = FirebaseFirestore.getInstance()
    var name by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add New Company", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("Logo URL") })
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (name.isNotBlank() && logoUrl.isNotBlank()) {
                val data = hashMapOf(
                    "name" to name,
                    "logoUrl" to logoUrl
                )
                db.collection("companies").document(name).set(data)
                    .addOnSuccessListener { message = "Company added successfully" }
                    .addOnFailureListener { message = "Failed to add company" }
            } else {
                message = "Please fill all fields"
            }
        }) {
            Text("Submit")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
    }
}

@Composable
fun AddQuestionScreen() {
    val db = FirebaseFirestore.getInstance()
    var companyList by remember { mutableStateOf(listOf<String>()) }
    var selectedCompany by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("companies").get().addOnSuccessListener { result ->
            companyList = result.documents.mapNotNull { it.id }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add Question", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        DropdownMenuWithLabel(
            label = "Select Company",
            options = companyList,
            selectedOption = selectedCompany,
            onOptionSelected = { selectedCompany = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Link") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated)") })
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (selectedCompany.isNotBlank() && title.isNotBlank()) {
                val questionData = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "link" to link,
                    "difficulty" to difficulty,
                    "tags" to tags.split(",").map { it.trim() }
                )
                db.collection("companies").document(selectedCompany)
                    .collection("questions").add(questionData)
                    .addOnSuccessListener { message = "Question added successfully" }
                    .addOnFailureListener { message = "Failed to add question" }
            } else {
                message = "Fill required fields"
            }
        }) {
            Text("Submit")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
    }
}

@Composable
fun DropdownMenuWithLabel(
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
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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