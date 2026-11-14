package com.cx.cakepet

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
    private val screen: () -> Rect, initialIndex: Int
) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).roundToInt()
    private val accent = 0xFFFFDB8A.toInt()
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(0xE6222222.toInt()); cornerRadius = dp(6).toFloat()
            setStroke(dp(1), 0x88666666.toInt())
        }
        setPadding(dp(7), dp(4), dp(7), dp(5))
    }
    private val header = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 11f
        setTypeface(null, android.graphics.Typeface.BOLD)
        includeFontPadding = false
        setPadding(0, dp(3), 0, dp(5))
    }
    private val body = TextView(context).apply {
        setTextColor(0xFFF0F0F0.toInt())
        textSize = 10f
        includeFontPadding = false
        setLineSpacing(0f, 1.0f)
    }
    private val scroll = ScrollView(context).apply {
        isFillViewport = false
        isVerticalScrollBarEnabled = false
        addView(body, ScrollView.LayoutParams(-1, -2))
    }
    private val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.LEFT
        width = minOf(dp(340), (screen().width() - dp(8)).coerceAtLeast(1))
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = screen().left + dp(4 + (initialIndex % 4) * 12)
        y = screen().top + dp(44 + (initialIndex % 6) * 24)
    }
    private var attached = false
    private var lastText = ""
    private var lastScreen = Rect()
    private var lastDesiredHeight = -1
    init {
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        var downX = 0f; var downY = 0f
        var originX = 0; var originY = 0
        header.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; originX = lp.x; originY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = originX + (e.rawX - downX).roundToInt()
                    lp.y = originY + (e.rawY - downY).roundToInt()
                    clampAndUpdate(true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }
    fun open(): Boolean {
        if (attached) return true
        return try {
            wm.addView(root, lp); attached = true; true
        } catch (e: Exception) {
            android.util.Log.w("PetDebugWindow", "无法创建调试窗口", e); false
        }
    }
    fun close() {
        if (!attached) return
        attached = false
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
        lp.x = lp.x.coerceIn(b.left, (b.right - lp.width).coerceAtLeast(b.left))
        val height = if (lastDesiredHeight > 0) lastDesiredHeight else root.height.coerceAtLeast(dp(24))
        lp.y = lp.y.coerceIn(b.top, (b.bottom - height).coerceAtLeast(b.top))
        if (force || oldX != lp.x || oldY != lp.y) {
            try { wm.updateViewLayout(root, lp) } catch (_: Exception) {}
        }
    }
    private fun fitWindow() {
        val b = screen()
        val newWidth = minOf(dp(340), (b.width() - dp(8)).coerceAtLeast(1))
        val widthChanged = lp.width != newWidth
        lp.width = newWidth
        val availWidth = (newWidth - root.paddingLeft - root.paddingRight).coerceAtLeast(1)
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(availWidth, android.view.View.MeasureSpec.EXACTLY)
        val unspecified = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        header.measure(widthSpec, unspecified)
        val maxBody = (b.height() - dp(12) - root.paddingTop - root.paddingBottom - header.measuredHeight)
            .coerceAtLeast(dp(24))
        body.textSize = 10f
        body.measure(widthSpec, unspecified)
        if (body.measuredHeight > maxBody) {
            body.textSize = 9f
            body.measure(widthSpec, unspecified)
        }
        if (body.measuredHeight > maxBody) {
            body.textSize = 8f
            body.measure(widthSpec, unspecified)
        }
        val targetHeight = minOf(body.measuredHeight, maxBody)
        val slp = scroll.layoutParams
        val heightChanged = slp.height != targetHeight
        if (heightChanged) { slp.height = targetHeight; scroll.layoutParams = slp }
        lastDesiredHeight = targetHeight + header.measuredHeight + root.paddingTop + root.paddingBottom
        lastScreen.set(b)
        clampAndUpdate(widthChanged || heightChanged)
    }
    private fun yes(v: Boolean) = if (v) "是" else "否"
    private fun on(v: Boolean) = if (v) "开" else "关"
    private fun side(v: Int) = when(v) { 0 -> "底"; 1 -> "左"; 2 -> "右"; 3 -> "顶"; else -> v.toString() }
    private fun n(v: Float) = if (v.isFinite()) v.roundToInt().toString() else v.toString()
    private fun xy(x: Float, y: Float) = "("+n(x)+","+n(y)+")"
    private fun path(p: List<String>?): String {
        if (p.isNullOrEmpty()) return "—"
        return p.mapIndexed { index, k ->
            val prefix = p.take(index+1).joinToString("/")
            when (prefix) {
                "standby/pool" -> "待机池"
                "standby/static_pool" -> "静止待机池"
                "snap/pool" -> "吸附待机池"
                "menu/pool" -> "手动选择"
                "pat_head/pat_head", "snap/pat_head/pat_head" -> "摸头"
                else -> when(k) {
                    "standby" -> "待机"; "snap" -> "吸附"; "snap_head" -> "探头"
                    "pat_head" -> "摸摸头"; "feed" -> "投食"; "eat" -> "吃"
                    "touch" -> "抛掷"; "roll" -> "滚动"; "lift_up" -> "提起"
                    "slow" -> "慢速"; "left_medium" -> "左中速"; "left_fast" -> "左快速"
                    "right_medium" -> "右中速"; "right_fast" -> "右快速"; "jump_down" -> "落地"
                    "climb" -> "攀岩"; "leap" -> "飞跃"; "landing" -> "降落"; "forage" -> "觅食"
                    "move" -> "移动"; "menu" -> "菜单动画"; "food" -> "食物"
                    else -> k
                }
            }
        }.joinToString("/")
    }
    private fun rect(p: WindowManager.LayoutParams?, added: Boolean): String =
        if (p == null) "—" else "[${p.x},${p.y},${p.x+p.width},${p.y+p.height}] ${p.width}×${p.height}${if (added) "" else " 未挂载"}"
    fun update(name: String, inst: PetInstance, count: Int, ime: Float, app: String?, fps: Float?) {
        if (!attached) return
        val v = inst.view; val p = v.physics
        val a = v.debugAnimationSnapshot(); val c = a.clip; val t = inst.petMsg.debugSnapshot()
        val title = "≡  ${name} · 调试 · 主实例 1/$count"
        val headerChanged = header.text.toString() != title
        if (headerChanged) header.text = title
        val out = SpannableStringBuilder()
        fun line(s: String) { if (out.isNotEmpty()) out.append('\n'); out.append(s) }
        fun heading(s: String) {
            if (out.isNotEmpty()) out.append('\n')
            val start = out.length; out.append("· ").append(s)
            out.setSpan(ForegroundColorSpan(accent), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        heading("功能与状态")
        line("功能："+path(a.function))
        val states = buildList {
            if (p.isDragging) add("提起")
            if (p.isThrowing) add("抛掷")
            if (p.isSnapped) add("吸附·"+side(p.snapSide))
            if (p.isClimbing) add("攀岩·"+side(p.climbSide))
            if (v.flying || p.inLeapFlight) add("飞跃")
            if (v.forageActive) add("觅食目标")
            if (isEmpty()) add(if (v.useStaticPool) "静止" else "普通")
        }
        line("状态：${states.joinToString(" / ")} · 强制：${yes(a.forced)}")
        line("状态池：${path(a.stateFunction)}")
        line("轮转：${if (a.forced && a.forcedReturn != null) "返回 "+a.forcedReturn else if (a.stateFunction != null) path(a.stateFunction) else path(a.standbyFunction)+" / 自动候选"}")
        heading("动画与帧")
        if (c == null) line("无动画") else {
            if (out.isNotEmpty()) out.append('\n')
            out.append("[")
            c.chain.ids.forEachIndexed { i, id ->
                if (i > 0) out.append(" > ")
                val start = out.length
                out.append(id)
                if (id == c.mode) out.setSpan(ForegroundColorSpan(accent), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (c.chain.terminal.isNotEmpty()) out.append(" ").append(c.chain.terminal)
            out.append("] ${c.chain.ids.indexOf(c.mode)+1}/${c.chain.ids.size}")
            line("${c.mode} · ${c.frame}/${c.frames}帧 · ${c.frameElapsed}/${c.frameDuration}ms")
            val policy = when(c.loop) { "off" -> "单次"; "timed" -> "定时循环"; "infinite" -> "无限循环"; else -> c.loop }
            val range = if (c.loop == "timed") " · ${c.durationMin ?: "—"}–${c.durationMax ?: c.durationMin ?: "—"}ms" else ""
            line("$policy$range · ${if(c.ended) "已结束" else "播放中"}")
            line("动画时长：${c.elapsed}/${c.target ?: "—"}ms · 循环${c.cycles}次")
            line("后续：${if(c.loop == "infinite") "—（无限）" else c.next ?: "功能轮转"} · ${if(c.waiting) "等待整轮" else if(c.finishCycle && c.loop == "timed") "整轮结束" else "—"}")
            line("XY(${if(c.x < 0) "—" else c.x},${c.y ?: "—"}) · dXY(${c.dx},${c.dy}) · 累计(${c.accumulatedX},${c.accumulatedY})")
            line("填充：${c.align} · 帧变换：${c.transform}")
            line("资源：${c.resource}")
        }
        heading("碎碎念")
        val policy = when(t.policy) { "cycle" -> "轮播"; "hold" -> "持续"; "off" -> "不播放"; else -> t.policy }
        line("$policy · ${a.thinkingSource} · ${t.poolSize}个文本")
        line("${t.id ?: "—"} · ${t.phase} · ${if(t.displayed) "可见" else "隐藏"} · 顶替：${yes(t.suppressed)}")
        line("阶段：${t.elapsed}/${if(t.locked) "∞" else t.target}ms · 剩余${if(t.locked) "∞" else (t.target-t.elapsed).coerceAtLeast(0)}")
        line("文本：${t.content ?: "—"}")
        heading("物理与窗口（px）")
        line("位置：${xy(p.x,p.y)} · 速度：${xy(p.vx,p.vy)}")
        line("锚点偏移：${xy(p.anchorOffX,p.anchorOffY)} · 旋转：${n(v.debugRenderRotation())}°")
        line("重力：${on(p.gravityEnabled)} · ${n(p.gravity)} · 镜像：${yes(v.debugRenderMirror())}")
        line("画布：${a.canvasW}×${a.canvasH} · 缩放：${"%.2f".format(java.util.Locale.US,v.getScale())}×")
        line("显示窗：${rect(inst.displayLp,inst.displayAdded)}")
        line("控制窗：${rect(inst.layoutParams,inst.controlAdded)}")
        line("活动 X[${n(p.minX)},${n(p.maxX)}] Y[${n(p.minY)},${n(p.maxY)}]")
        line("吸附 X[${n(p.snapMinX)},${n(p.snapMaxX)}] Y[${n(p.snapMinY)},${n(p.snapMaxY)}]")
        heading("交互与环境")
        val hit = when(v.hitMode) { ConfigDefaults.HIT_PIXEL -> "像素"; ConfigDefaults.HIT_CORE -> "核心"; else -> "边界" }
        val action = when(v.debugLastAction) { MotionEvent.ACTION_DOWN -> "DOWN"; MotionEvent.ACTION_MOVE -> "MOVE"; MotionEvent.ACTION_UP -> "UP"; MotionEvent.ACTION_CANCEL -> "CANCEL"; else -> "—" }
        line("$hit 命中 · $action · ${v.debugLastHitTest?.let { if(it) "命中" else "穿透" } ?: "—"}")
        line("键盘：${n(ime)}px · 主循环：${fps?.let { n(it) } ?: "—"}Hz")
        line("App：${app ?: "—"}")
        line("素材包：${a.version.ifBlank { "—" }} · ${a.packId}")
        line("软件：${AppVersion.name}")
        val raw = out.toString()
        val changed = raw != lastText || body.text.toString() != raw
        if (changed) { body.text = out; lastText = raw }
        if (changed || headerChanged || screen() != lastScreen) fitWindow()
    }
}
