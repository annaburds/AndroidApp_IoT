package com.example.simple_ble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.simple_ble.ui.theme.Simple_bleTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class DataDisplayActivity : ComponentActivity() {

    private val bleViewModel: BleViewModel
        get() = ViewModelSingleton.bleViewModel
            ?: throw IllegalStateException("BleViewModel is not initialized")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Simple_bleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val receivedData by bleViewModel.receivedData.collectAsState(initial = "No data received")
                    var numberOfCoughs by remember { mutableStateOf(2) } // Replace with actual value later
                    var floatData by remember { mutableStateOf(emptyList<Float>()) }

                    // Parse received data into floats
                    LaunchedEffect(receivedData) {
                        receivedData?.let {
                            if (it != "No data received") {
                                floatData = it.split(", ").mapNotNull { item -> item.toFloatOrNull() }
                            }
                        }
                    }


                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = "Received Data",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Number of Coughs: $numberOfCoughs",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Plot the data if available
                        if (floatData.isNotEmpty()) {
                            LineChartView(floatData)
                        } else {
                            Text(
                                text = "No plot available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LineChartView(data: List<Float>) {
        AndroidView(factory = { context ->
            val chart = LineChart(context)

            // Create entries for the data
            val entries = data.mapIndexed { index, value ->
                Entry(index * 0.3f / data.size, value) // Scale x-axis to 0.3 seconds
            }

            val dataSet = LineDataSet(entries, "Audio Signal")
            dataSet.color = android.graphics.Color.BLUE
            dataSet.setCircleColor(android.graphics.Color.RED)
            dataSet.setDrawCircles(false)

            val lineData = LineData(dataSet)
            chart.data = lineData

            // Customize chart
            chart.description = Description().apply { text = "Time (s)" }
            chart.xAxis.setDrawLabels(true)
            chart.axisLeft.setDrawLabels(true)
            chart.axisRight.isEnabled = false
            chart.legend.isEnabled = false

            chart.invalidate() // Refresh chart

            chart
        }, modifier = Modifier
            .fillMaxWidth()
            .height(300.dp))
    }
}
