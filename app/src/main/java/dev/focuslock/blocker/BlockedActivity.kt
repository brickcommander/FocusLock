package dev.focuslock.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            FocusLockTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A0000),
                                    Color(0xFF7F1D1D),
                                    Color(0xFFDC2626),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Lock icon circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                        ) {
                            Text("🔒", fontSize = 44.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "BLOCKED",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "by FocusLock",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Reason chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = reason.value,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = ::dismiss,
                            modifier = Modifier.fillMaxWidth(0.6f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFDC2626),
                            ),
                        ) {
                            Text(
                                text = if (isSiteBlock) "Go back" else "OK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (isSiteBlock) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Close or switch the tab",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
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
