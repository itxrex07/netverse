package app.netmirror.nmv2.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class NetMirrorJsBridge(
    private val onShowToast: (String) -> Unit,
    private val onSetRefreshRate: (Int) -> Boolean,
    private val onOpenSettings: () -> Unit,
    private val onExitApp: () -> Unit,
    private val onPlayerStateChanged: (Boolean) -> Unit,
    private val onContentReady: (Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post { onShowToast(message) }
    }

    @JavascriptInterface
    fun setRefreshRate(rate: Int): Boolean {
        return onSetRefreshRate(rate)
    }

    @JavascriptInterface
    fun openSettings() {
        mainHandler.post { onOpenSettings() }
    }

    @JavascriptInterface
    fun exitApp() {
        mainHandler.post { onExitApp() }
    }

    @JavascriptInterface
    fun onPlayerStateChanged(active: Boolean) {
        mainHandler.post { onPlayerStateChanged(active) }
    }

    @JavascriptInterface
    fun onContentReady(ready: Boolean) {
        mainHandler.post { onContentReady(ready) }
    }
}
