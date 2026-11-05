package ru.gigapisar.insertion

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Finds the messenger's "Send" button next to the field the text went into, so a key
 * press can tap it. Buttons are recognised by their label: what the app tells screen
 * readers ("Отправить", "Send message"). Among several the one nearest the field wins.
 */
internal object SendButton {
    private val sendWords = listOf("отправить", "послать", "send", "submit")

    /** Buttons that send something else, or later: never these. */
    private val notThese =
        listOf(
            "голос", "voice", "файл", "file", "фото", "photo", "видео", "video",
            "позже", "later", "отложен", "schedule", "запис", "record", "без звука", "silent",
            "стикер", "sticker", "gif", "местополож", "location", "контакт", "contact",
        )

    /** Whether a button with this label sends the message. */
    fun matches(label: CharSequence?): Boolean {
        val text = label?.toString()?.trim()?.lowercase() ?: return false
        if (text.isEmpty() || text.length > 40) return false
        if (notThese.any { it in text }) return false
        return sendWords.any { text == it || text.startsWith("$it ") }
    }

    /** The clickable node to tap, or null when the window has no send button. */
    fun find(
        root: AccessibilityNodeInfo,
        field: Rect?,
    ): AccessibilityNodeInfo? {
        val found = ArrayList<AccessibilityNodeInfo>()
        collect(root, found, 0)
        if (found.isEmpty()) return null
        if (field == null || found.size == 1) return found.first()
        val fieldCenter = intArrayOf(field.centerX(), field.centerY())
        return found.minByOrNull { node ->
            val r = Rect().also(node::getBoundsInScreen)
            val dx = (r.centerX() - fieldCenter[0]).toLong()
            val dy = (r.centerY() - fieldCenter[1]).toLong()
            dx * dx + dy * dy
        }
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        into: MutableList<AccessibilityNodeInfo>,
        depth: Int,
    ) {
        if (depth > 40) return
        if (node.isVisibleToUser && node.isEnabled && (matches(node.contentDescription) || matches(node.text))) {
            clickable(node)?.let { into.add(it) }
        }
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            collect(child, into, depth + 1)
        }
    }

    /** The node itself or the nearest ancestor that takes a click. */
    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops < 4) {
            if (current.isClickable) return current
            current = try { current.parent } catch (_: Exception) { null }
            hops++
        }
        return null
    }
}
