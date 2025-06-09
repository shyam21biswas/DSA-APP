package com.example.dsaadmin



import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow



data class SolvedQuestion(
    val title: String = "",
    val timestamp: String = ""
)

class RecentSolvedViewModel : ViewModel() {

    private val _recentSolved = MutableStateFlow<List<SolvedQuestion>>(emptyList())
    val recentSolved: StateFlow<List<SolvedQuestion>> = _recentSolved.asStateFlow()

    fun loadRecentSolved(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId).collection("recentSolved").document("last5")

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val array = document["questions"] as? List<Map<String, String>> ?: emptyList()
                val list = array.mapNotNull {
                    val title = it["title"]
                    val timestamp = it["timestamp"]
                    if (title != null && timestamp != null)
                        SolvedQuestion(title, timestamp)
                    else null
                }.reversed() // show latest first
                _recentSolved.value = list
            }
        }
    }

    fun addSolvedQuestion(userId: String, title: String) {
        val timestamp = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
        val newQuestion = mapOf("title" to title, "timestamp" to timestamp)

        val docRef = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("recentSolved").document("last5")

        docRef.get().addOnSuccessListener { document ->
            val currentList = (document["questions"] as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()

            currentList.add(newQuestion)
            if (currentList.size > 5) {
                currentList.removeAt(0) // remove oldest
            }

            docRef.set(mapOf("questions" to currentList))
                .addOnSuccessListener {
                    _recentSolved.value = currentList.reversed().map {
                        SolvedQuestion(it["title"] ?: "", it["timestamp"] ?: "")
                    }
                }
        }
    }
}


//data class SolvedQuestion(
//    val title: String = "",
//    val timestamp: String = ""
//)
//
//class RecentSolvedViewModel : ViewModel() {
//
//    private val _recentSolved = MutableStateFlow<List<SolvedQuestion>>(emptyList())
//    val recentSolved: StateFlow<List<SolvedQuestion>> = _recentSolved.asStateFlow()
//
//    fun loadRecentSolved(userId: String) {
//        val db = FirebaseFirestore.getInstance()
//        db.collection("users").document(userId).collection("recentSolved")
//            .orderBy("timestamp", Query.Direction.DESCENDING)
//            .limit(5)
//            .get()
//            .addOnSuccessListener { snapshot ->
//                val list = snapshot.documents.mapNotNull { doc ->
//                    val title = doc.getString("title")
//                    val timestamp = doc.getString("timestamp")
//                    if (title != null && timestamp != null) {
//                        SolvedQuestion(title, timestamp)
//                    } else null
//                }
//                _recentSolved.value = list
//            }
//    }
//
//    fun addSolvedQuestion(userId: String, title: String) {
//        val timestamp = SimpleDateFormat("hh:mm a, dd MMM yyyy",  Locale.getDefault()).format(Date())
//        val question = SolvedQuestion(title, timestamp)
//
//        val ref = FirebaseFirestore.getInstance()
//            .collection("users").document(userId)
//            .collection("recentSolved")
//
//        // Add new question
//        ref.add(question)
//            .addOnSuccessListener {
//                // Reload and trim to 5
//                loadRecentSolved(userId)
//                // Trim logic — ensure only last 5 remain
//                ref.orderBy("timestamp", Query.Direction.DESCENDING).get().addOnSuccessListener { snapshot ->
//                    val extra = snapshot.documents.drop(5)
//                    extra.forEach { it.reference.delete() }
//                }
//            }
//    }
//}


@Composable
fun RecentSolvedScreen(viewModel: RecentSolvedViewModel, userId: String) {
    val recentSolved by viewModel.recentSolved.collectAsState()

    // Load only once per composition
    LaunchedEffect(userId) {
        viewModel.loadRecentSolved(userId)
    }

    Column(Modifier.padding(16.dp)) {
        Text("🕑 Last 5 Solved Questions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(recentSolved) { question ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(question.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            question.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
