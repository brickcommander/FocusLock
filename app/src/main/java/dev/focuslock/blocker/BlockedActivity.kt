package dev.focuslock.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.focuslock.ui.theme.FocusLockTheme

/**
 * "Blocked" screen.
 * - App / Settings blocks: OK and Back go to the Home screen (never back to the blocked app).
 * - Site blocks: OK goes **back** to the browser so the user can close or switch the tab.
 *   A grace period prevents re-blocking for a few seconds.
 */
class BlockedActivity : ComponentActivity() {

    private val reason = mutableStateOf("")
    private var isSiteBlock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateFromIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        })

        setContent {
            FocusLockTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "🔒 Blocked by FocusLock",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = reason.value,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = ::dismiss) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent) {
        reason.value = intent.getStringExtra(EXTRA_REASON).orEmpty()
        isSiteBlock = intent.getBooleanExtra(EXTRA_IS_SITE, false)
    }

    private fun dismiss() {
        if (isSiteBlock) {
            // Go back to the browser so the user can close or switch the tab.
            // The grace period in DecisionThrottle prevents instant re-blocking.
            gracePeriodUntilMs = System.currentTimeMillis() + GRACE_MS
            finish()
        } else {
            startActivity(homeIntent())
            finish()
        }
    }

    companion object {
        private const val EXTRA_REASON = "dev.focuslock.extra.REASON"
        private const val EXTRA_IS_SITE = "dev.focuslock.extra.IS_SITE"
        private const val GRACE_MS = 10_000L

        /** After pressing OK on a site block, the service skips site blocks for this long. */
        @Volatile
        @JvmStatic
        var gracePeriodUntilMs: Long = 0L
            private set

        fun homeIntent(): Intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        fun intent(context: Context, reason: String, isSiteBlock: Boolean = false): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_IS_SITE, isSiteBlock)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
