package com.cx.cakepet

import android.graphics.Bitmap

/** 宠物专用的状态选择及动作生命周期，通用 ImageModeManager 不参与任何角色业务判断。 */
class PetAnimationController(private val assets: ImageModeManager) {
    private var currentFunctionPath: List<String> =
        if (assets.leavesOf("standby", "pool").isNotEmpty()) listOf("standby", "pool")
        else listOf("standby", "static_pool")
    private var currentName = assets.leavesOf(*currentFunctionPath.toTypedArray()).randomOrNull() ?: ""
    private var normalFunctionPath = currentFunctionPath
    private var clip: ImageModeManager.ModeClip? = assets.createClip(currentName)
    private var forced = false
    private var forcedReturn: String? = null
    private var forcedStart: String? = null
    // Publish the initial function even if its first animation never ends.
    private var modeChanged = true
    private val pendingFunctionForMode = mutableMapOf<String, List<String>>()
    private var frameMotionPending = true
    /** Active state-owned Functions path. A finished action chain resumes from this pool. */
    private var activeStateFunction: List<String>? = null
    /** Engine may choose an automatic function at a natural action boundary. */
    var onAutomaticModeChoice: (() -> String?)? = null

    fun setActiveStateFunction(path: List<String>?) {
        activeStateFunction = path
    }

    private fun activeStateMode(): String? =
        activeStateFunction?.let { path -> modeOf(*path.toTypedArray()) }

    var useStaticPool = false
    var forageActive = false
    /** Set only when the normal action chooser starts the forage behavior. */
    var forageSelected = false
    var inSnapped = false
    var isFlying = false

    /** Engine may choose an initial facing when the normal pool selects a moving clip. */
    var onStandbyMotionChosen: (() -> Unit)? = null

    val globalW: Int get() = assets.globalW
    val globalH: Int get() = assets.globalH
    fun globalMaxSize(): Pair<Int, Int> = assets.globalMaxSize()
    fun currentBitmapSize(): Pair<Int, Int> = assets.currentBitmapSize()
    fun currentBitmap(): Bitmap? = clip?.currentBitmap()
    fun currentAnchor(): Pair<Int, Int> = clip?.currentAnchor() ?: (80 to 160)
    fun previewFrame(res: String): Bitmap? = assets.previewFrame(res)
    fun currentResName(): String? = clip?.currentResName
    fun curDx(): Int = clip?.currentDx ?: 0
    fun curDy(): Int = clip?.currentDy ?: 0
    fun getMode(): String = clip?.currentModeId ?: ""
    fun isPlayingForced(): Boolean = forced
    fun executingSnapFunction(): Boolean = currentFunctionPath.firstOrNull() == "snap"
    fun executingFunction(path: List<String>): Boolean = currentFunctionPath == path
    fun executingDragFunction(): Boolean = currentFunctionPath.take(2) == listOf("touch", "lift_up")
    fun executingStandbyFunction(): Boolean = currentFunctionPath.firstOrNull() == "standby"
    fun executingForage(): Boolean = currentFunctionPath == listOf("climb", "forage", "move")

    /** A validated state entry is an unconditional takeover, even during an event. */
    fun enterStateFunction(path: List<String>): Boolean {
        val next = modeOf(*path.toTypedArray()) ?: return false
        forced = false
        forcedReturn = null
        forcedStart = null
        activeStateFunction = path
        setMode(next)
        return true
    }

    /** State exit is not an event. Select from the current environment, not the old clip. */
    fun resumeState(path: List<String>?) {
        activeStateFunction = path
        forced = false
        forcedReturn = null
        forcedStart = null
        val next = activeStateMode() ?: selectInitialStandby()
        next?.let { setMode(it) }
    }
    fun executingFoodLure(): Boolean = currentFunctionPath == listOf("feed", "lure") ||
        currentFunctionPath == listOf("snap", "feed", "lure")
    fun executingFoodLureFailure(): Boolean = currentFunctionPath == listOf("feed", "lure_fail") ||
        currentFunctionPath == listOf("snap", "feed", "lure_fail")
    fun startFoodLure(path: List<String>): Boolean {
        if (!enterStateFunction(path)) return false
        forageSelected = false
        return true
    }

    fun endFoodLure(resumePath: List<String>?, resume: Boolean) {
        // Clear the old state before choosing anything, otherwise an infinite lure can
        // restart itself or a completed eat can fall back into the old lure pool.
        activeStateFunction = resumePath
        if (!resume || !executingFoodLure()) return
        forced = false
        forcedReturn = null
        forcedStart = null
        val next = activeStateMode() ?: selectInitialStandby()
        next?.let { setMode(it) }
    }
    /** Frame impulse is consumed once for initial frame, every advance and loop wrap. */
    fun consumeFrameDisplacement(): Pair<Int, Int>? {
        if (!frameMotionPending) return null
        frameMotionPending = false
        return curDx() to curDy()
    }
    fun leavesOf(vararg path: String): List<String> = assets.leavesOf(*path)
    fun modeOf(vararg path: String): String? = assets.modeOf(*path)?.also {
        pendingFunctionForMode[it] = path.toList()
    }
    internal fun debugSnapshot(): AnimationDebugSnapshot {
        val catalog = assets.currentPack.thinking
        val mode = catalog.modes[getMode()]
        val source = when {
            mode != null && mode.policy != "inherit" -> "动画覆盖"
            currentFunctionPath in catalog.functions -> "继承功能"
            else -> "默认（空池）"
        }
        return AnimationDebugSnapshot(clip?.debugSnapshot(), currentFunctionPath,
            activeStateFunction, standbyPath(), forced, forcedReturn, source,
            assets.currentPack.version, assets.currentPack.id, assets.globalW, assets.globalH)
    }

    fun currentThinkingConfig(): Pair<ThinkingConfig, Map<String, String>> {
        val catalog = assets.currentPack.thinking
        return catalog.resolve(getMode(), currentFunctionPath) to catalog.texts
    }
    fun isFunctionMode(name: String, vararg path: String): Boolean = assets.isFunctionMode(name, *path)
    fun isDragMode(name: String = getMode()): Boolean =
        listOf("slow", "left_medium", "left_fast", "right_medium", "right_fast")
            .any { isFunctionMode(name, "touch", "lift_up", it) }

    private fun standbyPath(): List<String> =
        if (useStaticPool) listOf("standby", "static_pool") else listOf("standby", "pool")
    private fun currentPool(): List<String> = leavesOf(*standbyPath().toTypedArray())
    /** Reset/initialization selects a standby candidate, never a hard-coded default animation. */
    private fun selectInitialStandby(): String? {
        val primary = currentPool()
        val available = if (primary.isNotEmpty()) primary else
            leavesOf("standby", if (useStaticPool) "pool" else "static_pool")
        val selected = available.randomOrNull() ?: return null
        pendingFunctionForMode[selected] = if (primary.isNotEmpty()) standbyPath() else
            listOf("standby", if (useStaticPool) "pool" else "static_pool")
        return selected
    }
    fun isStandbyMode(name: String = getMode()): Boolean = currentPool().any { assets.isContinuationOf(name, it) }
    fun pickStandbyMode(exclude: String): String {
        // Only this natural completion boundary may initiate automatic behaviors.
        forageSelected = false
        if (forageActive && !useStaticPool && !inSnapped && !isFlying) modeOf("climb", "forage", "move")?.let {
            forageSelected = true
            return it
        }
        // Behavior decisions (e.g. whether to leap) belong to PetView, not Modes.
        onAutomaticModeChoice?.invoke()?.let { return it }
        val all = currentPool()
        val pool = all.filter { it != exclude }
        val selected = pool.randomOrNull() ?: all.randomOrNull()
            ?: selectInitialStandby() ?: getMode()
        // A single-candidate pool must not accidentally replay the previous event.
        if (all.isNotEmpty()) pendingFunctionForMode[selected] = standbyPath()
        if (assets.createClip(selected)?.hasFrameMotion == true) onStandbyMotionChosen?.invoke()
        return selected
    }

    /** A lure is a fresh normal function choice, not a forced-mode override.
     * Ignore current Mode, nextMode and forcedReturn: the whole old chain is
     * considered finished. State-owned functions still take priority.
     */
    fun tryRestartForageFromNormalChoice(): Boolean {
        val path = listOf("climb", "forage", "move")
        if (!forageActive || useStaticPool || inSnapped || isFlying) return false
        if (activeStateFunction != null && activeStateFunction != path) return false
        // Repeated contact with another food is not a new state entry.
        if (forageSelected && executingForage()) return false
        if (!enterStateFunction(path)) return false
        forageSelected = true
        return true
    }

    // Only a new function selection changes the context. A clip's nextMode
    // is part of the same execution, irrespective of other function bindings.
    private fun consumeFunction(id: String, explicitPath: List<String>? = null): List<String> {
        val selected = explicitPath ?: pendingFunctionForMode[id]
        pendingFunctionForMode.clear()
        return selected ?: emptyList()
    }

    fun setMode(id: String) {
        if (forced) return
        val next = assets.createClip(id) ?: return
        clip = next
        currentFunctionPath = consumeFunction(id)
        normalFunctionPath = currentFunctionPath
        currentName = id
        modeChanged = true
        frameMotionPending = true
    }

    fun playOnce(id: String, endMode: String? = null, functionPath: List<String>? = null) {
        val next = assets.createClip(id) ?: return
        clip = next
        currentFunctionPath = consumeFunction(id, functionPath)
        forced = true
        forcedReturn = endMode
        forcedStart = id
        modeChanged = true
        frameMotionPending = true
    }

    fun cancelForced() {
        if (!forced) return
        forced = false
        forcedReturn = null
        forcedStart = null
        clip = assets.createClip(currentName)
        currentFunctionPath = normalFunctionPath
        pendingFunctionForMode.clear()
        modeChanged = true
        frameMotionPending = true
    }

    fun forceReset() {
        activeStateFunction = null
        forageSelected = false
        forced = false
        forcedReturn = null
        forcedStart = null
        selectInitialStandby()?.let { setMode(it) }
    }

    fun setPack(pack: SpritePack) {
        if (assets.currentPack.id == pack.id) return
        assets.setPack(pack)
        forced = false
        forcedReturn = null
        forcedStart = null
        currentName = ""
        activeStateFunction = null
        pendingFunctionForMode.clear()
        currentFunctionPath = standbyPath()
        normalFunctionPath = currentFunctionPath
        selectInitialStandby()?.let { setMode(it) }
    }

    /** Physical landing wins over every unfinished flight animation or nextMode chain.
     * The landing function is an independent one-shot; its own Mode chain then plays
     * normally. Missing optional mappings fall back to ordinary automatic selection.
     */
    fun onLeapLanded() {
        val landing = modeOf("climb", "leap", "landing")
        activeStateFunction = null
        if (landing != null) {
            playOnce(landing, functionPath = listOf("climb", "leap", "landing"))
        } else {
            resumeState(null)
        }
    }

    fun didModeChange(): Boolean = modeChanged.also { modeChanged = false }

    /** 每帧只调用通用动画游标，待机池、飞跃等行为仍由宠物控制器决定。 */
    fun nextFrame(dtMs: Long): ImageModeManager.FrameStep {
        val current = clip ?: return ImageModeManager.FrameStep(false)
        // Physical flight never pauses a Mode's own frame timer or nextMode chain.
        val step = current.advance(dtMs)
        if (step.modeChanged) {
            // A Mode transition is internal to the same function, even when the
            // destination is also mapped by another function or loops forever.
            if (!forced) {
                currentName = current.currentModeId
                normalFunctionPath = currentFunctionPath
            }
            modeChanged = true
            frameMotionPending = true
        }
        if (step.changed) {
            frameMotionPending = true
        }
        if (step.ended || step.timerRequested) {
            val endMode = if (forced) forcedReturn else null
            forced = false
            forcedReturn = null
            forcedStart = null
            // A caller-specified return is a resume of the previous normal
            // execution, not an arbitrary reverse lookup of the returned Mode.
            if (endMode != null) pendingFunctionForMode[endMode] = normalFunctionPath
            val desired = endMode ?: activeStateMode() ?: pickStandbyMode(getMode())
            if (desired == current.currentModeId) {
                // Re-enter the same mode without replacing the cursor or resetting heading.
                current.restart()
                currentFunctionPath = consumeFunction(desired)
                normalFunctionPath = currentFunctionPath
                currentName = desired
                modeChanged = true
                frameMotionPending = true
            } else {
                setMode(desired)
            }
            return ImageModeManager.FrameStep(true)
        }
        return ImageModeManager.FrameStep(step.changed)
    }

    fun clearCache() = assets.clearCache()
}
