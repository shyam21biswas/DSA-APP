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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color

object RecentSolvedManager {
    private val queue: ArrayDeque<SolvedQuestion> = ArrayDeque()

    data class SolvedQuestion(
        val title: String,
        val timestamp: String
    )

    fun addSolvedQuestion(title: String) {
        val time = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
        val question = SolvedQuestion(title, time)

        queue.addFirst(question)
        if (queue.size > 4) {
            queue.removeLast()
        }
    }

    fun getRecentSolved(): List<SolvedQuestion> {
        return queue.toList()
    }

    fun clearQueue() {
        queue.clear()
    }
}



@Composable
fun RecentSolvedListUI() {
    val recentQuestions = remember { mutableStateListOf<RecentSolvedManager.SolvedQuestion>() }

    LaunchedEffect(Unit) {
        recentQuestions.clear()
        recentQuestions.addAll(RecentSolvedManager.getRecentSolved())
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("🕑 Last 5 Solved Questions", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (recentQuestions.isEmpty()) {
            Text("No recent questions yet.")
        } else {
            LazyColumn {
                items(recentQuestions) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = question.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = question.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
