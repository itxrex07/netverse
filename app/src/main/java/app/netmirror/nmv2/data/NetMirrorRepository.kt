package app.netmirror.nmv2.data

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

class NetMirrorRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val CUSTOM_USER_AGENT_SUFFIX = "/OS.Gatu v3.1.0.0.43"

        val PRIMARY_MIRRORS = listOf(
            "https://net52.cc/mobile/home",
            "https://netmirror.app",
            "https://net20.cc/mobile/home"
        )

        val CHECK_PHP_HOSTS = listOf(
            "https://mobidetect.cc",
            "https://mobidetects.cc",
            "https://mobidetect.live",
            "https://mobidetects.live",
            "https://mobidetect.click",
            "https://mobidetect.pro",
            "https://mobidetects.pro",
            "https://mobidetect.store",
            "https://mobidetects.store",
            "https://mobidetect.vip",
            "https://mobidetect.xyz",
            "https://mobidetects.xyz",
            "https://mobiledetects.com"
        )
    }

    @Serializable
    private data class SettingResponseDto(
        val hd: String = ""
    )

    suspend fun resolveActiveMirror(cachedMirrorUrl: String?): Result<String> =
        withContext(Dispatchers.IO) {
            val candidates = buildList {
                if (!cachedMirrorUrl.isNullOrBlank()) add(cachedMirrorUrl)
                addAll(PRIMARY_MIRRORS)
            }.distinct()

            for (url in candidates) {
                if (probeUrl(url)) {
                    return@withContext Result.success(url)
                }
            }

            // Fallback: check /check.php endpoints from the original bundle
            for (host in CHECK_PHP_HOSTS) {
                val resolved = probeCheckPhp(host)
                if (!resolved.isNullOrBlank()) {
                    return@withContext Result.success(resolved)
                }
            }

            Result.failure(
                IllegalStateException("Unable to reach any NetMirror gateway.")
            )
        }

    private fun probeUrl(url: String): Boolean {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) $CUSTOM_USER_AGENT_SUFFIX")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful || resp.code in 300..399
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun probeCheckPhp(host: String): String? {
        return try {
            val req = Request.Builder()
                .url("$host/check.php")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) $CUSTOM_USER_AGENT_SUFFIX")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string()?.trim().orEmpty()
                when {
                    body.startsWith("http://") || body.startsWith("https://") -> body.lines().first().trim()
                    body.contains("http") -> Regex("""https?://[^\s"'<>]+""").find(body)?.value
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun toggleHdSetting(activePageUrl: String?, currentHdState: Boolean): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val targetOrigin = extractOrigin(activePageUrl) ?: "https://net52.cc"
            val cookieManager = CookieManager.getInstance()
            val existingCookies = runCatching { cookieManager.getCookie(targetOrigin) }.getOrNull().orEmpty()

            return@withContext try {
                val requestBuilder = Request.Builder()
                    .url("$targetOrigin/mobile/setting.php")
                    .post(FormBody.Builder().add("hd", "hd").build())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) $CUSTOM_USER_AGENT_SUFFIX")

                if (existingCookies.isNotBlank()) {
                    requestBuilder.header("Cookie", existingCookies)
                }

                client.newCall(requestBuilder.build()).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && raw.isNotBlank()) {
                        val parsed = json.decodeFromString<SettingResponseDto>(raw)
                        val isHdOn = parsed.hd.equals("on", ignoreCase = true)
                        syncHdCookie(targetOrigin, isHdOn)
                        Result.success(isHdOn)
                    } else {
                        val nextState = !currentHdState
                        syncHdCookie(targetOrigin, nextState)
                        Result.success(nextState)
                    }
                }
            } catch (_: Exception) {
                val nextState = !currentHdState
                syncHdCookie(targetOrigin, nextState)
                Result.success(nextState)
            }
        }

    fun syncHdCookie(origin: String, isHdOn: Boolean) {
        runCatching {
            val cm = CookieManager.getInstance()
            val value = if (isHdOn) "hd=on; path=/" else "hd=off; path=/"
            cm.setCookie(origin, value)
            cm.flush()
        }
    }

    fun readHdCookie(activePageUrl: String?): Boolean? {
        val origin = extractOrigin(activePageUrl) ?: return null
        val cookie = runCatching { CookieManager.getInstance().getCookie(origin) }.getOrNull() ?: return null
        return when {
            cookie.contains("hd=on", ignoreCase = true) -> true
            cookie.contains("hd=off", ignoreCase = true) -> false
            else -> null
        }
    }

    private fun extractOrigin(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = URI(url)
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            if (uri.port != -1) "$scheme://$host:${uri.port}" else "$scheme://$host"
        } catch (_: Exception) {
            null
        }
    }
}
