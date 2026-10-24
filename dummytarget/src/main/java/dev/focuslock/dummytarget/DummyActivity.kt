package dev.focuslock.dummytarget

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class DummyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Dummy target opened"
            textSize = 24f
        }
        setContentView(tv)
    }
}
