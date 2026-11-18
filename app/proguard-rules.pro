# Keep JavascriptInterface methods for WebView bridge
-keepclassmembers class app.netmirror.nmv2.webview.NetMirrorJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
