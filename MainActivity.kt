package com.junwoo.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.mozilla.geckoview.*

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession
    private lateinit var runtime: GeckoRuntime
    private lateinit var urlBox: EditText

    private var desktopMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        geckoView = findViewById(R.id.geckoView)
        urlBox = findViewById(R.id.urlBox)

        runtime = GeckoRuntime.create(this)

        session = GeckoSession(GeckoSessionSettings.Builder().build())
        session.open(runtime)
        geckoView.setSession(session)

        setupBrowser()
        setupUI()
        setupDownload()
        setupAdblock()
    }

    private fun setupBrowser() {
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                urlBox.setText(session.currentUri)
            }
        }
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.goBtn).setOnClickListener {
            var u = urlBox.text.toString()
            if (!u.startsWith("http")) u = "https://$u"
            session.loadUri(u)
        }

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            if (session.canGoBack) session.goBack()
        }

        findViewById<ImageButton>(R.id.forwardBtn).setOnClickListener {
            if (session.canGoForward) session.goForward()
        }

        findViewById<ImageButton>(R.id.refreshBtn).setOnClickListener {
            session.reload()
        }

        findViewById<ImageButton>(R.id.homeBtn).setOnClickListener {
            session.loadUri("https://google.com")
        }

        findViewById<ImageButton>(R.id.desktopBtn).setOnClickListener {
            desktopMode = !desktopMode

            session.settings = session.settings.toBuilder()
                .userAgentMode(
                    if (desktopMode)
                        GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else
                        GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                )
                .build()

            Toast.makeText(this, "Desktop Mode: $desktopMode", Toast.LENGTH_SHORT).show()
            session.reload()
        }

        findViewById<ImageButton>(R.id.shareBtn).setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, session.currentUri)
            startActivity(Intent.createChooser(i, "공유하기"))
        }
    }

    private fun setupDownload() {
        session.setDownloadDelegate(object : GeckoSession.DownloadDelegate {
            override fun onDownload(
                session: GeckoSession,
                download: GeckoSession.DownloadDelegate.Download
            ): GeckoResult<String>? {

                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                val filename = download.filename ?: "download"
                val req = DownloadManager.Request(Uri.parse(download.uri))
                    .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        filename
                    )

                manager.enqueue(req)
                return null
            }
        })
    }

    private fun setupAdblock() {
        session.webRequestDelegate = object : GeckoSession.WebRequestDelegate {
            override fun onBeforeRequest(
                session: GeckoSession,
                request: GeckoSession.WebRequestDelegate.Request
            ): GeckoResult<GeckoSession.WebRequestDelegate.Action>? {

                val url = request.uri

                if (url.contains("doubleclick") ||
                    url.contains("banner") ||
                    url.contains("ads.") ||
                    url.contains("adservice")
                ) {
                    return GeckoResult.fromValue(
                        GeckoSession.WebRequestDelegate.Action.CANCEL
                    )
                }
                return null
            }
        }
    }
}
