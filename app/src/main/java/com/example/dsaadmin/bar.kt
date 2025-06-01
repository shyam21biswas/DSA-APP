package com.example.dsaadmin

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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



@Composable
fun BarChartScreen(userId: String, onDismiss: () -> Unit) {
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
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📊 Daily Solved Questions", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(16.dp))
        BarChartView(stats)
    }
}

@Composable
fun BarChartView(stats: Map<String, Int>) {
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

            chart.data = BarData(dataSet)

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
}
