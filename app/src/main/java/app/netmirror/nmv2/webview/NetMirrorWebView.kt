package app.netmirror.nmv2.webview

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.netmirror.nmv2.data.NetMirrorRepository

class WebViewController {
    internal var webView: WebView? = null

    fun currentUrl(): String? = webView?.url

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack() {
        webView?.goBack()
    }

    fun reload() {
        webView?.reload()
    }

    fun evaluatePressFromApp(onResult: (handled: Boolean) -> Unit) {
        val view = webView
        if (view == null) {
            onResult(false)
            return
        }
        view.evaluateJavascript(
            "(function(){ if (typeof window.pressfromAPP === 'function') { return window.pressfromAPP(); } return false; })();"
        ) { result ->
            val handled = result?.trim()?.equals("true", ignoreCase = true) == true
            onResult(handled)
        }
    }

    fun syncHdCookieToWebView(isHdOn: Boolean) {
        val view = webView ?: return
        val value = if (isHdOn) "hd=on" else "hd=off"
        view.evaluateJavascript(
            """
            (function(){
              document.cookie = "$value; path=/";
              var cb = document.getElementById('nm-hd-checkbox');
              var st = document.getElementById('nm-hd-status');
              if (cb) cb.checked = ${if (isHdOn) "true" else "false"};
              if (st) st.textContent = ${if (isHdOn) "'High-bitrate 1080p active'" else "'Standard definition streaming'"};
            })();
            """.trimIndent(),
            null
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NetMirrorWebView(
    url: String,
    controller: WebViewController,
    jsBridge: NetMirrorJsBridge,
    onPageStarted: () -> Unit,
    onPageFinished: (String?) -> Unit,
    onPageError: (String) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val enhancerScript = runCatching {
                context.assets.open("enhancer.js").bufferedReader().use { it.readText() }
            }.getOrNull().orEmpty()

            val nativeSettingsBridgeHook = """
                (function() {
                  if (window.__NM_NATIVE_SETTINGS_HOOK__) return;
                  window.__NM_NATIVE_SETTINGS_HOOK__ = true;
                  document.addEventListener('click', function(e) {
                    var link = e.target && e.target.closest ? e.target.closest('.bottom-navigation-container a.nav-link') : null;
                    if (!link) return;
                    if (link.classList.contains('setting-btn') || (link.textContent && link.textContent.trim().toLowerCase().indexOf('setting') !== -1)) {
                      if (window.NetMirrorNative && window.NetMirrorNative.openSettings) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        window.NetMirrorNative.openSettings();
                      }
                    }
                  }, true);
                  document.addEventListener('click', function(e) {
                    if (e.target && e.target.closest) {
                      if (e.target.closest('.model-btn-play2, .btn-play2, .playerstart')) {
                        if (window.NetMirrorNative && window.NetMirrorNative.onPlayerStateChanged) {
                          window.NetMirrorNative.onPlayerStateChanged(true);
                        }
                      }
                      if (e.target.closest('.btn-payer-back')) {
                        if (window.NetMirrorNative && window.NetMirrorNative.onPlayerStateChanged) {
                          window.NetMirrorNative.onPlayerStateChanged(false);
                        }
                      }
                    }
                  }, true);
                })();
            """.trimIndent()

            val container = FrameLayout(context).apply {
                setBackgroundColor(Color.parseColor("#060608"))
            }

            val fullscreenContainer = FrameLayout(context).apply {
                setBackgroundColor(Color.BLACK)
                visibility = View.GONE
            }

            var customView: View? = null
            var customViewCallback: WebChromeClient.CustomViewCallback? = null

            val webView = WebView(context).apply {
                setBackgroundColor(Color.parseColor("#060608"))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "$userAgentString ${NetMirrorRepository.CUSTOM_USER_AGENT_SUFFIX}"
                }

                addJavascriptInterface(jsBridge, "NetMirrorNative")

                val blockedAdHosts = listOf(
                    "userver.",
                    "doubleclick.net",
                    "googlesyndication.com",
                    "googleadservices.com",
                    "adservice.google",
                    "popads.net",
                    "popcash.net",
                    "propellerads.",
                    "adsterra.",
                    "highperformanceformat.com",
                    "profitablegatecpm.com",
                    "topcreativeformat.com"
                )

                fun isBlockedAdUrl(targetUrl: String?): Boolean {
                    val lower = targetUrl?.lowercase() ?: return false
                    return blockedAdHosts.any { lower.contains(it) }
                }

                fun injectEnhancer(view: WebView?) {
                    if (view == null) return
                    if (enhancerScript.isNotBlank()) {
                        view.evaluateJavascript(enhancerScript, null)
                    }
                    view.evaluateJavascript(nativeSettingsBridgeHook, null)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return isBlockedAdUrl(request?.url?.toString())
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        if (isBlockedAdUrl(request?.url?.toString())) {
                            return android.webkit.WebResourceResponse(
                                "text/plain",
                                "utf-8",
                                java.io.ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, pageUrl, favicon)
                        onPageStarted()
                    }

                    override fun onPageCommitVisible(view: WebView?, pageUrl: String?) {
                        super.onPageCommitVisible(view, pageUrl)
                        injectEnhancer(view)
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        injectEnhancer(view)
                        onPageFinished(pageUrl)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString().orEmpty().ifBlank {
                                "Connection interrupted"
                            }
                            onPageError(desc)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        // Drop popup ad windows instead of opening them
                        return false
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view == null) return
                        if (customView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        customView = view
                        customViewCallback = callback
                        fullscreenContainer.addView(
                            view,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        fullscreenContainer.visibility = View.VISIBLE
                        onFullscreenChanged(true)
                    }

                    override fun onHideCustomView() {
                        customView?.let { fullscreenContainer.removeView(it) }
                        customView = null
                        fullscreenContainer.visibility = View.GONE
                        customViewCallback?.onCustomViewHidden()
                        customViewCallback = null
                        onFullscreenChanged(false)
                    }
                }

                loadUrl(url)
            }

            controller.webView = webView
            container.addView(webView)
            container.addView(
                fullscreenContainer,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            container
        },
        update = { frame ->
            val wv = controller.webView
            if (wv != null && wv.url.isNullOrBlank()) {
                wv.loadUrl(url)
            }
        }
    )
}
