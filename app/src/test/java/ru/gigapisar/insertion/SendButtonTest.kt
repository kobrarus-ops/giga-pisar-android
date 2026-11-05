package ru.gigapisar.insertion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendButtonTest {
    @Test
    fun `send labels of common messengers match`() {
        assertTrue(SendButton.matches("Отправить"))
        assertTrue(SendButton.matches("Отправить сообщение"))
        assertTrue(SendButton.matches("Send"))
        assertTrue(SendButton.matches("Send message"))
        assertTrue(SendButton.matches("  send  "))
    }

    @Test
    fun `buttons that send something else do not match`() {
        assertFalse(SendButton.matches("Отправить голосовое сообщение"))
        assertFalse(SendButton.matches("Отправить файл"))
        assertFalse(SendButton.matches("Отправить позже"))
        assertFalse(SendButton.matches("Записать голосовое сообщение"))
        assertFalse(SendButton.matches("Send photo"))
        assertFalse(SendButton.matches("Schedule send"))
        assertFalse(SendButton.matches("Sender"))
        assertFalse(SendButton.matches("Отправитель"))
        assertFalse(SendButton.matches(null))
        assertFalse(SendButton.matches(""))
    }
}
