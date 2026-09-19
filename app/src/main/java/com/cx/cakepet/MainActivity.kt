package com.cx.cakepet

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.cx.cakepet.databinding.ActivityMainBinding
import com.cx.cakepet.AppVersion

/**
 * 入口 Activity：浮窗权限引导 + 多角色卡片（召唤 / 归位 / 召回 / 设置）。
 * 对应 PC 端无（PC 直接运行），安卓必须做 SYSTEM_ALERT_WINDOW 引导。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshCards() }

    /** 读取 PetService 持久化的退出意图，避免进程重启后误把已退出的宠物复活。 */
    private fun wasExited(): Boolean {
        val prefs = getSharedPreferences("cakepet_exit", MODE_PRIVATE)
        return prefs.getBoolean("exiting", false)
    }

    /** 用户明确启动宠物时，清除先前的退出意图。 */
    private fun clearExited() {
        getSharedPreferences("cakepet_exit", MODE_PRIVATE)
            .edit().putBoolean("exiting", false).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvAuthor.text = "初心丶CX  ·  v${AppVersion.name}"
        buildRoleCards()
    }

    override fun onResume() {
        super.onResume()
        // 注册角色在屏状态变化回调：服务异步召唤/召回后即时刷新卡片（无需重新进页面）。
        PetService.onActiveRolesChanged = {
            runOnUiThread { if (::binding.isInitialized && !isFinishing && !isDestroyed) refreshCards() }
        }
        refreshCards()
    }

    override fun onPause() {
        // 取消注册，避免泄漏/对已销毁页面回调。
        PetService.onActiveRolesChanged = null
        super.onPause()
    }

    /** 为每个预装/上传角色构建一张卡片。 */
    private fun buildRoleCards() {
        binding.roleList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (role in RoleRegistry.all()) {
            val card = inflater.inflate(R.layout.item_role_card, binding.roleList, false)
            card.tag = role.id
            card.findViewById<TextView>(R.id.role_name).text = role.displayName
            val thumb = loadThumb(role)
            if (thumb != null) card.findViewById<ImageView>(R.id.role_thumb).setImageBitmap(thumb)
            card.findViewById<Button>(R.id.btn_summon).setOnClickListener { onSummon(role) }
            card.findViewById<Button>(R.id.btn_settings).setOnClickListener { onSettings(role) }
            card.findViewById<Button>(R.id.btn_recall).setOnClickListener { onRecall(role) }
            binding.roleList.addView(card)
        }
        refreshCards()
    }

    /** 根据当前已召唤角色刷新各卡片状态。 */
    private fun refreshCards() {
        val svc = PetService.instance
        for (i in 0 until binding.roleList.childCount) {
            val card = binding.roleList.getChildAt(i)
            val id = card.tag as? String ?: continue
            val activeHere = svc?.isRoleActive(id) == true
            card.findViewById<TextView>(R.id.role_status).text = if (activeHere) "● 已召唤" else "○ 未召唤"
            card.findViewById<Button>(R.id.btn_summon).text = if (activeHere) "归位" else "召唤"
            // 固定比例：未召唤时「召唤」占 2 份宽度（≈两个按钮宽），「召回」隐藏 → 视觉 [召唤, 设置]；
            // 已召唤时「召唤」拆成「归位」(1 份) + 「召回」(1 份) + 「设置」(1 份) → 视觉 [归位, 召回, 设置]，设置始终居右。
            val summonLp = card.findViewById<Button>(R.id.btn_summon).layoutParams as LinearLayout.LayoutParams
            summonLp.weight = if (activeHere) 1f else 2f
            card.findViewById<Button>(R.id.btn_summon).layoutParams = summonLp
            card.findViewById<Button>(R.id.btn_recall).visibility = if (activeHere) View.VISIBLE else View.GONE
        }
    }

    /** 召唤/归位同一按钮：未召唤→召唤；已召唤→归位。单活跃角色（切换会先召回旧的）。 */
    private fun onSummon(role: RoleDescriptor) {
        if (!hasOverlayPermission()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayLauncher.launch(intent)
            return
        }
        clearExited()
        if (PetService.instance != null) {
            PetService.instance?.summonRole(role.id)
        } else {
            startForegroundService(Intent(this, PetService::class.java).apply {
                putExtra(PetService.EXTRA_ROLE_ID, role.id)
            })
        }
        refreshCards()
    }

    /** 召回（退场）：撤窗、停循环，回到未召唤；不卸载、不丢配置。 */
    private fun onRecall(role: RoleDescriptor) {
        PetService.instance?.recall(role.id)
        refreshCards()
    }

    /** 设置：打开对应角色的专属设置页（role_id 透传，写入 pet_config_<id> / pet_bounds_<id>）。 */
    private fun onSettings(role: RoleDescriptor) {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra("role_id", role.id)
        })
    }

    /** 解码角色预览帧（assets/<rootPath>/<thumbRes>），失败回退占位。 */
    private fun loadThumb(role: RoleDescriptor): android.graphics.Bitmap? {
        return try {
            val path = "${role.rootPath}/${role.thumbRes}"
            // 先仅读尺寸，自适应决定采样率：小图（如芝麻球促销包仅 128px）不二次降采样以免更糊；
            // 大图（如芝麻酥/白酥 1000px）按目标约 256px 降采样，省内存又清晰。
            val justBounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it, null, justBounds) }
            val maxDim = maxOf(justBounds.outWidth, justBounds.outHeight).coerceAtLeast(1)
            val target = 256
            var sample = 1
            while (maxDim / (sample + 1) >= target) sample++
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) { null }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    /** 判断 PetService 是否正在运行 */
    private fun isServiceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE).any { it.service.className == PetService::class.java.name }
    }
}
