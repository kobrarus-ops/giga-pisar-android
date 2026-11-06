package ru.gigapisar.text

import org.junit.Assert.assertEquals
import org.junit.Test

class HesitationsTest {
    @Test
    fun soundAtTheStartOfASentenceGoesWithItsPunctuation() {
        assertEquals(
            "Вот. Просто сразу, чтобы они мгновенно появлялись",
            Hesitations.removed("Вот. А-а просто сразу, чтобы они мгновенно появлялись"),
        )
        assertEquals("Понял тебя.", Hesitations.removed("А-а, понял тебя."))
        assertEquals("Ты где? Я уже тут.", Hesitations.removed("Ты где? А-а, я уже тут."))
        assertEquals("Ну ладно.", Hesitations.removed("Эм, ну ладно."))
        assertEquals("Давай завтра", Hesitations.removed("Э-э... давай завтра"))
        assertEquals("Первая\nВторая", Hesitations.removed("Первая\nМ-м, вторая"))
    }

    @Test
    fun soundInsideASentenceLeavesOneComma() {
        assertEquals(
            "Есть сообщения, которые мы не сокращаем.",
            Hesitations.removed("Есть сообщения, э-э, которые мы не сокращаем."),
        )
        assertEquals("Я думаю завтра", Hesitations.removed("Я эээ думаю ммм завтра"))
        assertEquals("Давай завтра.", Hesitations.removed("Давай завтра, э-э."))
        assertEquals("Давай завтра", Hesitations.removed("Давай завтра, э-э-э"))
        assertEquals("Купи хлеб, молоко", Hesitations.removed("Купи хлеб, э-э, м-м, молоко"))
    }

    @Test
    fun nothingButSoundsIsAnEmptyTake() {
        assertEquals("", Hesitations.removed("Э-э-э."))
        assertEquals("", Hesitations.removed("М-м, э-э..."))
    }

    @Test
    fun wordsAndUnitsStayAsTheyAre() {
        for (text in listOf(
            "А я думаю, что да.",
            "Длина 5 мм.",
            "Это эхо, поэтому мама ушла.",
            "Скажи «э-э» вслух.",
            "Привет, как дела?",
        )) {
            assertEquals(text, Hesitations.removed(text))
        }
    }
}
