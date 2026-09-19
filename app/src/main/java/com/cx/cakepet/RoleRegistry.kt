package com.cx.cakepet

/**
 * 角色注册表：当前仅预装角色（顺序即首页卡片顺序）。
 * 将来用户上传包 = 在 [scanUserRoles] 里扫描 filesDir/roles/<id>/ 含合法 modes.json 的包，
 * 并入 [PRESETS]；引擎（PetRole）与 [RoleDescriptor] 结构不变，故无需为未知角色改动底层。
 */
object RoleRegistry {

    /** 预装角色（写死）。 */
    val PRESETS: List<RoleDescriptor> = listOf(
        RoleDescriptor(
            id = "zhimasu",
            displayName = "芝麻酥",
            rootPath = "芝麻酥",
            packId = 1,
            // 预览图位于「安静」子目录下（根目录无 sit_clam-1.png），路径须含子目录否则首页预览为空。
            thumbRes = "安静/sit_clam-1.png"
        ),
        RoleDescriptor(
            id = "baiasu",
            displayName = "白酥",
            rootPath = "白酥",
            packId = 3,
            thumbRes = "安静/sit_clam-1.png"
        ),
        RoleDescriptor(
            id = "zhimaqiu",
            displayName = "芝麻球促销",
            rootPath = "芝麻球促销",
            packId = 2,
            thumbRes = "安静/sit_clam-1.png"
        )
    )

    /** 默认角色（首启动 / 旧版 pet_config 迁移目标）。 */
    val DEFAULT: RoleDescriptor = PRESETS.first()

    /** 全部角色（预装 ∪ 将来扫描所得上传包）。 */
    fun all(): List<RoleDescriptor> = PRESETS + scanUserRoles()

    fun byId(id: String): RoleDescriptor? = all().firstOrNull { it.id == id }

    /**
     * 预留：扫描用户导入的角色包。
     * 当前未启用（Phase: 用户上传素材），返回空。将来在此读取 filesDir/roles/<id>/modes.json
     * 并构造 [RoleDescriptor]（source=RoleSource.FILES，rootPath 指向私有存储目录）。
     */
    private fun scanUserRoles(): List<RoleDescriptor> = emptyList()
}
