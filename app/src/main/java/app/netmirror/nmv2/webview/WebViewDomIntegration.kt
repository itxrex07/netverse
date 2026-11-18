package app.netmirror.nmv2.webview

/**
 * Provides minimal WebView DOM integration hooks generated in Kotlin,
 * delegating Settings, Toast, Refresh Rate, Back navigation, and Ad-Gate
 * verification to native Kotlin components.
 */
object WebViewDomIntegration {

    val pageScript: String = """
        (function () {
          'use strict';
          if (window.__NM_NATIVE_BRIDGE_V2__) return;
          window.__NM_NATIVE_BRIDGE_V2__ = true;

          window.open = function () { return null; };

          if (!document.getElementById('nm-native-styles')) {
            var style = document.createElement('style');
            style.id = 'nm-native-styles';
            style.textContent = [
              '.info2, .adboxing, .dark-bg, #ad-banner-test, .appdownload, .app-download,',
              '.open-support, .support-box, .verify-box, .ad-gate, .ad-overlay,',
              '#setting-box, div[style*="max-width:700px"][style*="position:fixed"],',
              'div[style*="background:rgb(255,0,0)"] {',
              '  display: none !important; visibility: hidden !important; opacity: 0 !important;',
              '  pointer-events: none !important; height: 0 !important; max-height: 0 !important;',
              '  overflow: hidden !important;',
              '}',
              'html, body { background-color: #060608 !important; overscroll-behavior-y: contain !important; }',
              '#player, .player, .player-box, #jw { z-index: 99999 !important; }',
              'body.overflow3 .bottom-navigation-container,',
              'body.nm-player-active .bottom-navigation-container {',
              '  display: none !important; visibility: hidden !important; opacity: 0 !important;',
              '  pointer-events: none !important;',
              '}'
            ].join('\n');
            (document.head || document.documentElement).appendChild(style);
          }

          function notifyContentReady(ready) {
            if (window.NetMirrorNative && window.NetMirrorNative.onContentReady) {
              window.NetMirrorNative.onContentReady(!!ready);
            }
          }

          function checkPageReadiness() {
            var hasContent = !!document.querySelector('.tray-container, .bottom-navigation-container, #main .card, #main .poster');
            var hasGate = document.body && (
              document.body.hasAttribute('data-addhash') ||
              document.querySelector('.open-support') ||
              document.querySelector('.info2')
            );
            if (!hasGate || hasContent) {
              document.querySelectorAll('.open-support, .info2, .adboxing, .dark-bg').forEach(function (el) {
                el.remove();
              });
              notifyContentReady(true);
              return;
            }

            notifyContentReady(false);
            var addhash = document.body.getAttribute('data-addhash');
            if (!addhash) {
              notifyContentReady(true);
              return;
            }

            var attempts = 0;
            function pollVerify() {
              attempts++;
              fetch('/mobile/verify2.php', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                  'X-Requested-With': 'XMLHttpRequest'
                },
                body: 'verify=' + encodeURIComponent(addhash),
                credentials: 'include'
              })
              .then(function (r) { return r.json(); })
              .then(function (res) {
                if (res && res.statusup === 'All Done') {
                  clearInterval(timer);
                  location.replace('/mobile/home');
                }
              })
              .catch(function () {});

              if (attempts >= 8) {
                clearInterval(timer);
                location.replace('/mobile/home');
              }
            }
            pollVerify();
            var timer = setInterval(pollVerify, 500);
          }

          document.addEventListener('click', function (e) {
            var target = e.target;
            if (!target || !target.closest) return;

            var navLink = target.closest('.bottom-navigation-container a.nav-link');
            if (navLink && (navLink.classList.contains('setting-btn') ||
                (navLink.textContent && navLink.textContent.trim().toLowerCase().indexOf('setting') !== -1))) {
              if (window.NetMirrorNative && window.NetMirrorNative.openSettings) {
                e.preventDefault();
                e.stopImmediatePropagation();
                window.NetMirrorNative.openSettings();
              }
              return;
            }

            if (target.closest('.model-btn-play2, .btn-play2, .playerstart')) {
              var nav = document.querySelector('.bottom-navigation-container');
              if (nav) nav.style.setProperty('display', 'none', 'important');
              if (window.NetMirrorNative && window.NetMirrorNative.onPlayerStateChanged) {
                window.NetMirrorNative.onPlayerStateChanged(true);
              }
            }

            if (target.closest('.btn-payer-back')) {
              var navBar = document.querySelector('.bottom-navigation-container');
              if (navBar) navBar.style.removeProperty('display');
              if (window.NetMirrorNative && window.NetMirrorNative.onPlayerStateChanged) {
                window.NetMirrorNative.onPlayerStateChanged(false);
              }
            }
          }, true);

          window.pressfromAPP = function () {
            var playerEl = document.getElementById('player');
            var isPlayerVisible = playerEl && !playerEl.classList.contains('hide');
            var hasJw = window.$ && $("#jw").hasClass("jwplayer");
            if (isPlayerVisible || hasJw || (document.body && document.body.classList.contains('overflow3'))) {
              var backBtn = document.querySelector('.btn-payer-back');
              if (backBtn) {
                backBtn.click();
              } else if (window.$) {
                $("#player").addClass("hide");
                $(".body").removeClass("overflow3");
                if (typeof window.jwplayer === 'function') {
                  try { window.jwplayer().stop().remove(); } catch (err) {}
                }
              }
              var nav = document.querySelector('.bottom-navigation-container');
              if (nav) nav.style.removeProperty('display');
              return true;
            }
            if (window.$ && $("#exampleModalScrollable").hasClass("show")) {
              $(".btn-close2").click();
              return true;
            }
            if (window.$ && $(".search").is(":visible") && $(".search").css("left") === "0px") {
              $(".btn-search-close").click();
              return true;
            }
            var isHome = window.location.pathname.indexOf('/mobile/home') !== -1 || window.location.pathname === '/';
            if (window.history.length > 1 && !isHome) {
              window.history.back();
              return true;
            }
            return false;
          };

          if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', checkPageReadiness);
          } else {
            checkPageReadiness();
          }
        })();
    """.trimIndent()
}
