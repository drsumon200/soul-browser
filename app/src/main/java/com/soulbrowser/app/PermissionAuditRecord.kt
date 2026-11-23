package com.soulbrowser.app
data class PermissionAuditRecord(
    val host: String,
    val permissionName: String,
    val allowed: Boolean
)
