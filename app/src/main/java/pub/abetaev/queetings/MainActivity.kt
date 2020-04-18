package pub.abetaev.queetings

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.MotionEvent
import android.webkit.*


const val PERMISSION_REQUEST = 0x12345078

class MainActivity : Activity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requiredPermissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.BLUETOOTH
        )
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            run()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        val webView = findViewById<WebView>(R.id.webview)
        if (intent?.data != null) {
            val dataUri: Uri = intent.data!!
            val joinUrl = if (dataUri.scheme == "https") dataUri.getQueryParameter("join")
            else dataUri
            webView.evaluateJavascript("window.network.accept(new URL('${joinUrl}'))") {
                Log.d("OPEN", it)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val allGranted: Boolean = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted and (requestCode == PERMISSION_REQUEST)) {
            run()
        } else {
            finish()
        }
    }

    private fun run() {
        setContentView(R.layout.activity_webview)
        val webView = findViewById<WebView>(R.id.webview)
        val baseUrl = if (intent?.data != null) {
            intent.data?.toString()
        } else {
            getString(R.string.website_url)
        }
        setupWebView(webView)
        webView.loadUrl(baseUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupWebView(webView: WebView) {
        val settings: WebSettings = webView.settings

        // Enable Javascript
        settings.javaScriptEnabled = true

        // Enable auto play videos
        settings.mediaPlaybackRequiresUserGesture = false

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Enable pinch to zoom without the zoom buttons
        settings.builtInZoomControls = false

        // Allow use of Local Storage
        settings.domStorageEnabled = false

        // Hide the zoom controls for HONEYCOMB+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.displayZoomControls = false
        }

        // Enable remote debugging via chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Disable scrolling. At all!!!
        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.setOnTouchListener { _, event -> (event.action == MotionEvent.ACTION_MOVE); }



        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                webView.evaluateJavascript("window.webview = true") {
                    Log.d("SETUP", it)
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
        }

        val activity = this
        webView.webChromeClient = object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest) {
                activity.runOnUiThread {
                    when (request.origin.host) {
                        getString(R.string.website_domain) -> {
                            request.grant(request.resources)
                        }
                        else -> {
                            request.deny()
                        }
                    }
                }
            }
        }

    }

}