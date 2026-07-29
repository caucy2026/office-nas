package com.kemi.desklink.media

import java.net.URI
import java.net.URLDecoder

/**
 * URI-only network source entry for P3b. User-info, credential-bearing query values and
 * fragments are intentionally rejected because WorkspaceRepository persists MediaRef; a
 * later NAS credential store must use Keystore.
 */
data class NetworkMediaSource(
    val provider: String,
    /** Raw URI string; Android conversion happens only at MediaEngine's UI boundary. */
    val uri: String,
    val displayName: String,
)

object NetworkMediaUri {
    private val supportedSchemes = setOf("smb", "nfs", "upnp", "http", "https", "rtsp")
    private val sensitiveQueryNameFragments = setOf(
        "token",
        "signature",
        "credential",
        "password",
        "secret",
        "auth",
        "apikey",
        "api_key",
        "key",
        "sig",
    )

    fun parse(raw: String): Result<NetworkMediaSource> = runCatching {
        val uri = URI(raw.trim())
        val scheme = uri.scheme?.lowercase() ?: error("缺少协议，例如 smb:// 或 rtsp://")
        require(scheme in supportedSchemes) { "暂不支持 $scheme 协议" }
        require(uri.userInfo.isNullOrBlank()) { "URI 不能包含用户名或密码" }
        require(uri.rawFragment.isNullOrBlank()) { "URI 不能包含 fragment 凭据" }
        require(!hasSensitiveQuery(uri)) { "URI 不能包含 token、签名或其他凭据；请等待 Keystore Provider" }
        val host = uri.host?.takeIf { it.isNotBlank() } ?: error("缺少服务器地址")
        NetworkMediaSource(
            provider = scheme,
            uri = uri.toString(),
            displayName = uri.path
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: host,
        )
    }

    private fun hasSensitiveQuery(uri: URI): Boolean = uri.rawQuery
        ?.split('&')
        ?.map { entry ->
            URLDecoder.decode(entry.substringBefore('='), "UTF-8")
                .lowercase()
        }
        ?.any { name -> sensitiveQueryNameFragments.any(name::contains) }
        ?: false
}
