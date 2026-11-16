package com.cx.cakepet

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.WindowManager
import kotlin.math.roundToInt

internal data class PetDebugDocument(val title: String, val body: Spanned) {
    fun colored(accent: Int): Spanned {
        val result = SpannableString(body)
        for (span in result.getSpans(0, result.length, StyleSpan::class.java)) {
            result.setSpan(ForegroundColorSpan(accent), result.getSpanStart(span),
                result.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return result
    }
}

/** Capture once on the engine's main thread, then share immutable text between both UIs. */
internal object PetDebugFormatter {
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
    fun capture(name: String, inst: PetInstance, count: Int, ime: Float, app: String?, fps: Float?): PetDebugDocument {
        val v = inst.view; val p = v.physics
        val a = v.debugAnimationSnapshot(); val c = a.clip; val t = inst.petMsg.debugSnapshot()
        val title = "${name} · 调试 · 主实例 1/$count"
        val out = SpannableStringBuilder()
        fun line(s: String) { if (out.isNotEmpty()) out.append('\n'); out.append(s) }
        fun heading(s: String) {
            if (out.isNotEmpty()) out.append('\n')
            val start = out.length; out.append("· ").append(s)
            out.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
                if (id == c.mode) out.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        return PetDebugDocument(title, android.text.SpannedString(out))
    }
}
