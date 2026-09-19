package com.cx.cakepet

/**
 * 角色资源来源。
 * - [ASSETS]：打包进 APK 的预装角色（assets/<rootPath>/…）。
 * - [FILES]：用户导入/上传的角色（filesDir/roles/<id>/…，将来启用）。
 */
enum class RoleSource { ASSETS, FILES }

/**
 * 角色描述符（轻量元数据，不持有引擎实例）。
 * 预装角色写死；将来用户上传包 = 扫描 filesDir/roles/<id>/ 含合法 modes.json 者，
 * 转成同结构描述符并入 [RoleRegistry]，引擎（PetRole）完全无需改动。
 *
 * @param id        稳定唯一 id（也用作 DataStore 文件名后缀，如 pet_config_zhimasu / pet_bounds_zhimasu）。
 * @param displayName 首页卡片展示名。
 * @param rootPath  assets / 私有存储下该角色资源根目录（如 "芝麻酥" / "芝麻球促销"）。
 * @param packId    资源包逻辑标识（沿用旧 spritePack 映射：0=旧图 / 1=芝麻酥 / 2=芝麻球促销），
 *                  用于决定 decodeTarget / forceBottomCenterAnchor 等包特性。
 * @param thumbRes  首页卡片预览图 res（经 SpritePack.resolve 后的 resName，如 "sit_clam-1.png" / "安静/sit_clam-1.png"）。
 * @param source    资源来源。
 */
data class RoleDescriptor(
    val id: String,
    val displayName: String,
    val rootPath: String,
    val packId: Int,
    val thumbRes: String,
    val source: RoleSource = RoleSource.ASSETS
)
