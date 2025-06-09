package com.example.dsaadmin

import android.graphics.fonts.FontStyle
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.github.mikephil.charting.charts.LineChart


@Composable
fun BarChartScreen(userId: String, onDismiss: () -> Unit ) {
//    val stats = mapOf(
//        "May 25" to 2,
//        "May 26" to 4,
//        "May 27" to 6,
//        "May 28" to 3,
//        "May 29" to 5,
//        "May 30" to 7,
//        "May 31" to 1
//    )
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        fetchLast7DaysStats(userId) {
            stats = it
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)

    ) {
        Row {

            Text("📊 Daily Solved Questions", style = MaterialTheme.typography.h6)
           // Text("X", style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f).clickable {  onDismiss  }, textAlign = TextAlign.End)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss  ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        //Spacer(modifier = Modifier.height(16.dp))
        BarChartView(stats , userId)

    }
}

@Composable
fun BarChartView(stats: Map<String, Int> , userId: String) {

    Column() {
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        600
                    )
                    description.isEnabled = false
                    legend.isEnabled = false
                }
            },
            update = { chart ->
                val entries = stats.entries.mapIndexed { index, entry ->
                    BarEntry(index.toFloat(), entry.value.toFloat())
                }

                val dataSet = BarDataSet(entries, "Questions Solved").apply {
                    colors = ColorTemplate.MATERIAL_COLORS.toList()
                    valueTextSize = 12f
                }

                chart.data  = BarData(dataSet)

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(stats.keys.toList())
                    granularity = 1f
                    isGranularityEnabled = true
                    position = XAxis.XAxisPosition.BOTTOM
                    textSize = 12f
                }

                chart.axisRight.isEnabled = false
                chart.animateY(1000)
                chart.invalidate()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
       // val recentQuestions = remember { mutableStateListOf<RecentSolvedManager.SolvedQuestion>() }
        val viewModel: RecentSolvedViewModel = viewModel()
        val recentQuestions by viewModel.recentSolved.collectAsState()

//        LaunchedEffect(Unit) {
//            recentQuestions.clear()
//            recentQuestions.addAll(RecentSolvedManager.getRecentSolved())
//        }
        // Load data once on first composition
        LaunchedEffect(userId) {
            viewModel.loadRecentSolved(userId)
        }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 200.dp, max = 270.dp)){ // Limit max height) {


            Text("Recent Solved Questions", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            if (recentQuestions.isEmpty()) {
                Text("No recent questions yet.")
            } else {
                LazyColumn {
                    items(recentQuestions) { question ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            backgroundColor = Color(0x80CCFF8C)

                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(text = question.title, style = MaterialTheme.typography.body1)
                                Text(
                                    text = question.timestamp,
                                    style = MaterialTheme.typography.caption,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "From confusion to clarity — keep grinding! ✨",
            style = MaterialTheme.typography.body2.copy(
                //fontStyle = FontStyle.,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )




    }
}


