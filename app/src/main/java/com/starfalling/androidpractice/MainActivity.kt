package com.starfalling.androidpractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.UiThread
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import com.starfalling.androidpractice.ui.theme.AndroidPracticeTheme
import com.starfalling.androidpractice.ui.theme.SimpleDrawingSurface

class MainActivity : ComponentActivity(), InProgressStrokesFinishedListener {
    private lateinit var inProgressStrokesView: InProgressStrokesView
    private val finishedStrokesState = mutableStateOf(emptyList<Stroke>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inProgressStrokesView = InProgressStrokesView(this)
        inProgressStrokesView.addFinishedStrokesListener(this)

        enableEdgeToEdge()
        setContent {
            var brush by remember {
                mutableStateOf(
                    Brush.createWithColorIntArgb(
                        family = StockBrushes.pressurePenLatest,
                        colorIntArgb = Color.Black.toArgb(),
                        size = 5F,
                        epsilon = 0.1F
                    )
                )
            }
            AndroidPracticeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 30.dp)
                    ) {
                        Text(
                            text = "簡易簽名板",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = {
                                finishedStrokesState.value = emptyList() // 🧹 清空畫布
                            }) {
                                Text("清除畫布")
                            }
                            Button(onClick = {
                                if (finishedStrokesState.value.isNotEmpty()) {
                                    finishedStrokesState.value =
                                        finishedStrokesState.value.dropLast(1) // 移除最後一筆
                                }
                            }) {
                                Text("Undo")
                            }
                            Button(onClick = {
                                val eraserBrush = Brush.createWithColorIntArgb(
                                    family = StockBrushes.pressurePenLatest,
                                    colorIntArgb = Color.White.toArgb(),
                                    size = 20f,
                                    epsilon = 0.1f
                                )
                                brush = eraserBrush
                            }) {
                                Text("橡皮擦")
                            }
                        }
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "黑筆" to Color.Black,
                                "紅筆" to Color.Red,
                                "藍筆" to Color.Blue,
                                "綠筆" to Color.Green
                            ).forEach { (label, color) ->
                                Button(onClick = {
                                    brush = Brush.createWithColorIntArgb(
                                        family = StockBrushes.pressurePenLatest,
                                        colorIntArgb = color.toArgb(),
                                        size = 5F,
                                        epsilon = 0.1F
                                    )
                                }) {
                                    Text(text = label)
                                }
                            }
                        }
                        SimpleDrawingSurface(
                            modifier = Modifier
                                .padding(vertical = 20.dp)
                                .background(Color.White),
                            brush = rememberUpdatedState(brush),
                            inProgressStrokesView = inProgressStrokesView,
                            finishedStrokesState = finishedStrokesState
                        )
                    }
                }
            }
        }
    }

    @UiThread
    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        // 紀錄新筆畫
        finishedStrokesState.value = finishedStrokesState.value + strokes.values

        // 移除會畫結束的筆畫
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
    }
}