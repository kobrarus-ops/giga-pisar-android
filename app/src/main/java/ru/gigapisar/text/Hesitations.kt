package ru.gigapisar.text

/**
 * Cuts hesitation sounds out of a take: "э-э", "а-а", "м-м" and their spellings. Runs on the
 * phone, so it works with the Brain off and without a network. Words that are sometimes filler
 * and sometimes meaning ("вот", "ну", "короче") are not touched: that is the Brain's job.
 */
object Hesitations {
    /**
     * A lone "а" is a conjunction and "мм" is millimetres, so those two stay. A sound in
     * quotes is somebody talking about the sound itself.
     */
    private val sound =
        Regex(
            """(?<![\p{L}\p{N}«"„“-])""" +
                """(?:[эЭ]+(?:-[эЭ]+)*[мМ]*|[аА]+(?:-[аА]+)+|[аА]{3,}|[мМ]+(?:-[мМ]+)+|[мМ]{3,})""" +
                """(?![\p{L}\p{N}»"“”-])""",
        )

    private const val SENTENCE_ENDS = ".!?…"
    private const val AFTER_SOUND = ",.!?…"

    fun removed(text: String): String {
        if (!sound.containsMatchIn(text)) return text
        val out = StringBuilder()
        var at = 0
        var capitalize = false

        fun push(chunk: String) {
            if (chunk.isEmpty()) return
            out.append(if (capitalize) chunk.replaceFirstChar { it.uppercase() } else chunk)
            capitalize = false
        }

        for (match in sound.findAll(text)) {
            if (match.range.first < at) continue
            push(text.substring(at, match.range.first))
            at = match.range.last + 1
            val before = out.trimEnd(' ', '\t')
            val opensSentence = before.isEmpty() || before.last() in SENTENCE_ENDS || before.last() == '\n'
            if (opensSentence) {
                // "А-а, просто сразу" -> "Просто сразу": the sound goes with its own punctuation.
                while (at < text.length && text[at] in AFTER_SOUND) at++
                while (at < text.length && (text[at] == ' ' || text[at] == '\t')) at++
                if (match.value.first().isUpperCase()) capitalize = true
                continue
            }
            out.setLength(before.length)
            val next = text.getOrNull(at)
            if (out.last() == ',') {
                when {
                    // "сообщения, э-э, которые" -> "сообщения, которые": one comma is enough.
                    next == ',' -> at++
                    // "завтра, э-э." -> "завтра."
                    next == null || next in SENTENCE_ENDS -> out.setLength(out.length - 1)
                }
            }
        }
        push(text.substring(at))

        val cleaned = out.toString().replace(Regex("""[ \t]{2,}"""), " ").trim().trimEnd(',').trimEnd()
        return if (cleaned.any { it.isLetterOrDigit() }) cleaned else ""
    }
}
