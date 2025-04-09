package com.starfalling.androidpractice.ui.theme

import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

@Composable
fun SimpleDrawingSurface(
    modifier: Modifier = Modifier,
    brush: State<Brush> = mutableStateOf(
        Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePenLatest,
            colorIntArgb = Color.Black.toArgb(),
            size = 5F,
            epsilon = 0.1F
        )
    ),
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: State<List<Stroke>>
) {
    // 紀錄目前正在繪圖的手指ID 及筆畫 ID
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }
    val canvasStrokeRenderer = CanvasStrokeRenderer.create() // 用來把筆畫記錄到 canvas

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = object : FrameLayout(context) {
                    override fun performClick(): Boolean {
                        super.performClick()
                        return true
                    }
                }
                inProgressStrokesView.apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }
                // 提前預測使用者筆畫的下一個點，用來提升筆畫的流暢度
                val predictor = MotionEventPredictor.newInstance(rootView)

                val touchListener = View.OnTouchListener { view, event ->
                    predictor.record(event)
                    val predictedEvent = predictor.predict()

                    try {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                // First pointer - treat it as inking.

                                // 讓觸控事件不經過系統預設的buffer以提高筆畫的順暢度
                                // 平常手指在 scroll 時系統通常會進行緩衝以節省電量
                                view.requestUnbufferedDispatch(event)

                                // 取得目前的位置並開始繪製筆刷
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                currentPointerId.value = pointerId
                                currentStrokeId.value = inProgressStrokesView.startStroke(
                                    event = event,
                                    pointerId = pointerId,
                                    brush = brush.value
                                )
                                true
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val pointerId = currentPointerId.value ?: return@OnTouchListener true
                                val strokeId = currentStrokeId.value ?: return@OnTouchListener true

                                // 可能有多點觸控，所以要判斷多隻手指的情況
                                // 並且只針對我們正在會畫的那隻手指才增加筆畫
                                for (pointerIndex in 0 until event.pointerCount) {
                                    if (event.getPointerId(pointerIndex) != pointerId) continue
                                    inProgressStrokesView.addToStroke(
                                        event,
                                        pointerId,
                                        strokeId,
                                        predictedEvent
                                    )
                                }
                                true
                            }

                            MotionEvent.ACTION_UP -> {
                                // 取得手指id 並檢查是不是正在繪圖的那隻手指
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                if (pointerId != currentPointerId.value) return@OnTouchListener true

                                // 拿到筆畫 id，結束此筆繪圖
                                val strokeId = checkNotNull(currentStrokeId.value)
                                inProgressStrokesView.finishStroke(
                                    event,
                                    pointerId,
                                    strokeId
                                )
                                view.performClick() // 觸發點擊（支援 talkback & 無障礙動作）
                                true
                            }

                            MotionEvent.ACTION_CANCEL -> {
                                // 筆畫被取消的狀況
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                if (pointerId != currentPointerId.value) return@OnTouchListener true
                                val strokeId = currentStrokeId.value ?: return@OnTouchListener true
                                inProgressStrokesView.cancelStroke(strokeId, event)
                                true
                            }

                            else -> false
                        }
                    } catch (e: Exception) {
                        true
                    } finally {
                        predictedEvent?.recycle()
                    }
                }
                rootView.setOnTouchListener(touchListener)
                rootView.addView(inProgressStrokesView)
                rootView
            },
        )
        Canvas(modifier = Modifier) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            finishedStrokesState.value.forEach { stroke ->
                canvasStrokeRenderer.draw(stroke = stroke, canvas = canvas, strokeToScreenTransform = canvasTransform)
            }
        }
    }
}
