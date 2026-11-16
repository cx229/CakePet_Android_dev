package com.cx.cakepet

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

internal data class DebugChain(val ids: List<String>, val terminal: String)
internal data class ClipDebugSnapshot(
    val chain: DebugChain, val mode: String, val frame: Int, val frames: Int,
    val frameElapsed: Long, val frameDuration: Long, val loop: String,
    val durationMin: Long?, val durationMax: Long?, val elapsed: Long, val target: Long?,
    val cycles: Int, val finishCycle: Boolean, val waiting: Boolean, val next: String?,
    val ended: Boolean, val resource: String, val x: Int, val y: Int?, val dx: Int, val dy: Int,
    val align: String, val transform: String, val accumulatedX: Int, val accumulatedY: Int
)
internal data class AnimationDebugSnapshot(
    val clip: ClipDebugSnapshot?, val function: List<String>, val stateFunction: List<String>?,
    val standbyFunction: List<String>, val forced: Boolean, val forcedReturn: String?,
    val thinkingSource: String, val version: String, val packId: String,
    val canvasW: Int, val canvasH: Int
)
internal data class ThinkingDebugSnapshot(
    val policy: String, val poolSize: Int, val id: String?, val content: String?,
    val phase: String, val elapsed: Long, val target: Long, val locked: Boolean,
    val displayed: Boolean, val suppressed: Boolean
)

/** Read-only per-role diagnostic window. Drag the header; no extra disclosure controls. */
internal class PetDebugWindow(
    private val context: Context, private val wm: WindowManager,
    private val screen: () -> Rect, initialIndex: Int,
    private val onClose: () -> Unit
) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).roundToInt()
    private val accent = 0xFFFFDB8A.toInt()
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(0x66222222.toInt()); cornerRadius = dp(6).toFloat()
            setStroke(dp(1), 0x44666666.toInt())
        }
        setPadding(dp(7), dp(4), dp(7), dp(5))
    }
    private val header = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD)
        includeFontPadding = false
        setPadding(dp(4), dp(4), dp(4), dp(4))
        gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(40)
    }
    private val closeButton = TextView(context).apply {
        text = "×"
        textSize = 22f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        contentDescription = "关闭调试窗口"
        setOnClickListener { onClose() }
    }
    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(header, LinearLayout.LayoutParams(0, -2, 1f))
        addView(closeButton, LinearLayout.LayoutParams(dp(40), dp(40)))
    }
    private val body = TextView(context).apply {
        setTextColor(0xFFF0F0F0.toInt())
        textSize = 11f
        includeFontPadding = false
        setLineSpacing(0f, 1.0f)
    }
    private val scroll = ScrollView(context).apply {
        isFillViewport = false
        isVerticalScrollBarEnabled = false
        addView(body, android.widget.FrameLayout.LayoutParams(-1, -2))
    }
    private val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.LEFT
        width = minOf(dp(280), (screen().width() - dp(8)).coerceAtLeast(1))
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = screen().left + dp(4 + (initialIndex % 4) * 12)
        y = screen().top + dp(44 + (initialIndex % 6) * 24)
    }
    private var attached = false
    private var lastText = ""
    private var lastScreen = Rect()
    private var lastDocument: PetDebugDocument? = null
    private var dragging = false
    private var lastHeaderHeight = dp(40)
    init {
        root.addView(headerRow, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        var downX = 0f; var downY = 0f
        var originX = 0; var originY = 0
        header.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = true
                    downX = e.rawX; downY = e.rawY; originX = lp.x; originY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = originX + (e.rawX - downX).roundToInt()
                    lp.y = originY + (e.rawY - downY).roundToInt()
                    clampAndUpdate(true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    if (screen() != lastScreen) fitWindow()
                    true
                }
                else -> false
            }
        }
    }
    fun open(): Boolean {
        if (attached) return true
        return try {
            wm.addView(root, lp); attached = true; fitWindow(); true
        } catch (e: Exception) {
            android.util.Log.w("PetDebugWindow", "无法创建调试窗口", e); false
        }
    }
    fun close() {
        if (!attached) return
        attached = false
        dragging = false
        try { wm.removeViewImmediate(root) } catch (_: Exception) {}
    }
    fun bringToFront() {
        if (!attached) return
        close()
        open()
    }
    private fun clampAndUpdate(force: Boolean = false) {
        if (!attached) return
        val b = screen()
        val oldX = lp.x; val oldY = lp.y
        // Keep a reachable portion of the drag handle on screen, not the entire body.
        val visibleHandle = minOf(dp(64), lp.width)
        lp.x = lp.x.coerceIn(b.left - lp.width + visibleHandle, (b.right - visibleHandle).coerceAtLeast(b.left))
        lp.y = lp.y.coerceIn(b.top, (b.bottom - lastHeaderHeight - dp(8)).coerceAtLeast(b.top))
        if (force || oldX != lp.x || oldY != lp.y) {
            try { wm.updateViewLayout(root, lp) } catch (_: Exception) {}
        }
    }
    private fun fitWindow() {
        if (!attached || dragging) return
        val b = screen()
        lp.width = minOf(dp(280), (b.width() - dp(8)).coerceAtLeast(1))
        val availWidth = (lp.width - root.paddingLeft - root.paddingRight).coerceAtLeast(1)
        headerRow.measure(android.view.View.MeasureSpec.makeMeasureSpec(availWidth, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
        lastHeaderHeight = headerRow.measuredHeight.coerceAtLeast(dp(40))
        // Fixed viewport: changing frame counters must never resize or reposition the window.
        val maxBody = ((b.height() * 0.65f).roundToInt() - root.paddingTop -
            root.paddingBottom - lastHeaderHeight).coerceAtLeast(dp(24))
        scroll.layoutParams = scroll.layoutParams.apply { height = maxBody }
        lastScreen.set(b)
        clampAndUpdate(true)
    }
    fun update(document: PetDebugDocument) {
        if (!attached) return
        val title = "≡  " + document.title
        if (header.text.toString() != title) header.text = title
        val raw = document.body.toString()
        if (raw != lastText || lastDocument?.body != document.body) {
            body.text = document.colored(accent)
            lastText = raw
            lastDocument = document
        }
        if (!dragging && screen() != lastScreen) fitWindow()
    }
}
