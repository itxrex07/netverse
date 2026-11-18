package app.netmirror.nmv2.data

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NetMirrorRepository {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
            synchronized(hostCookies) {
                for (cookie in cookies) {
                    hostCookies.removeAll { it.name == cookie.name && it.path == cookie.path }
                    hostCookies.add(cookie)
                }
            }
            // Mirror all session cookies directly into Android WebView CookieManager
            val cm = runCatching { CookieManager.getInstance() }.getOrNull() ?: return
            val origin = "${url.scheme}://${url.host}"
            for (cookie in cookies) {
                cm.setCookie(origin, cookie.toString())
            }
            cm.flush()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host]?.toList().orEmpty()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val CUSTOM_USER_AGENT_SUFFIX = "/OS.Gatu v3.1.0.0.43"
        private val ADDHASH_REGEX = Regex("""data-addhash=["']([^"']+)["']""")

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

    @Serializable
    private data class VerifyResponseDto(
        val statusup: String = ""
    )

    suspend fun resolveActiveMirror(cachedMirrorUrl: String?): Result<String> =
        withContext(Dispatchers.IO) {
            val candidates = buildList {
                if (!cachedMirrorUrl.isNullOrBlank()) add(cachedMirrorUrl)
                addAll(PRIMARY_MIRRORS)
            }.distinct()

            for (url in candidates) {
                if (probeAndVerifyMirror(url)) {
                    return@withContext Result.success(url)
                }
            }

            // Fallback: check /check.php endpoints
            for (host in CHECK_PHP_HOSTS) {
                val resolved = probeCheckPhp(host)
                if (!resolved.isNullOrBlank()) {
                    probeAndVerifyMirror(resolved)
                    return@withContext Result.success(resolved)
                }
            }

            Result.failure(
                IllegalStateException("Unable to reach any NetMirror gateway.")
            )
        }

    /**
     * Probes the mirror URL in native Kotlin. If the server returns a data-addhash
     * verification gate, performs the POST /mobile/verify2.php handshake natively
     * and syncs the verified session cookies into WebView's CookieManager.
     */
    private suspend fun probeAndVerifyMirror(url: String): Boolean {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) $CUSTOM_USER_AGENT_SUFFIX")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code !in 300..399) return false
                val html = resp.body?.string().orEmpty()
                val addhash = ADDHASH_REGEX.find(html)?.groupValues?.getOrNull(1)
                if (!addhash.isNullOrBlank() && !html.contains("tray-container")) {
                    performNativeVerifyHandshake(resp.request.url.toString(), addhash)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun performNativeVerifyHandshake(pageUrl: String, addhash: String) {
        val origin = extractOrigin(pageUrl) ?: return
        repeat(4) { attempt ->
            val verified = runCatching {
                val req = Request.Builder()
                    .url("$origin/mobile/verify2.php")
                    .post(FormBody.Builder().add("verify", addhash).build())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) $CUSTOM_USER_AGENT_SUFFIX")
                    .build()

                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val dto = json.decodeFromString<VerifyResponseDto>(body)
                        dto.statusup == "All Done"
                    } else {
                        false
                    }
                }
            }.getOrDefault(false)

            if (verified) return
            if (attempt < 3) delay(350)
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
