package dev.focuslock.blocker

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Collects the texts of a window (depth-first, capped) for SettingsGuard. Never stored or logged. */
object ScreenTextReader {

    private const val MAX_NODES = 200

    fun read(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val texts = ArrayList<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            node.text?.takeIf { it.isNotBlank() }?.let { texts.add(it.toString()) }
            node.contentDescription?.takeIf { it.isNotBlank() }?.let { texts.add(it.toString()) }
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let(stack::addLast)
            if (node !== root) recycle(node)
        }
        stack.forEach(::recycle)
        return texts
    }

    @Suppress("DEPRECATION")
    private fun recycle(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) node.recycle()
    }
}
