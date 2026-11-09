package ru.gigapisar.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "own server" address: what a person types on the tab, and what a request uses. */
class BrainProvidersTest {
    @Test
    fun `a full address keeps its own path`() {
        val server = BrainProviders.customAt("https://plusvibeapi.ru/v1")
        assertEquals("https://plusvibeapi.ru/v1", server?.baseUrl)
        assertEquals("plusvibeapi.ru", server?.name)
        assertEquals("custom", server?.id)
    }

    @Test
    fun `a bare host gets the openai path`() {
        assertEquals("https://plusvibeapi.ru/v1", BrainProviders.customAt("https://plusvibeapi.ru")?.baseUrl)
        assertEquals("https://plusvibeapi.ru/v1", BrainProviders.customAt("  https://plusvibeapi.ru/  ")?.baseUrl)
    }

    @Test
    fun `a port and a long path are kept`() {
        assertEquals("https://192.168.1.5:1234/api/v1", BrainProviders.customAt("https://192.168.1.5:1234/api/v1")?.baseUrl)
    }

    @Test
    fun `plain http is refused`() {
        // The app has no cleartext traffic at all: Google Play Protect blocks such builds.
        assertNull(BrainProviders.customAt("http://192.168.1.5:1234/v1"))
        assertNull(BrainProviders.customAt("plusvibeapi.ru/v1"))
        assertNull(BrainProviders.customAt(""))
        assertNull(BrainProviders.customAt("   "))
    }

    @Test
    fun `endpoint puts the typed address in place of the table one`() {
        val used = BrainProviders.endpoint(BrainProviders.Custom, ollamaUrl = "", customUrl = "https://plusvibeapi.ru/v1")
        assertEquals("https://plusvibeapi.ru/v1", used?.baseUrl)
        // Nothing typed yet: there is no server to send the text to.
        assertNull(BrainProviders.endpoint(BrainProviders.Custom, ollamaUrl = "", customUrl = ""))
    }

    @Test
    fun `cloud services keep their own address`() {
        val used = BrainProviders.endpoint(BrainProviders.OpenAI, ollamaUrl = "", customUrl = "https://plusvibeapi.ru/v1")
        assertEquals(BrainProviders.OpenAI, used)
    }

    @Test
    fun `the own server is reachable by its id and is not offered among the clouds`() {
        assertEquals(BrainProviders.Custom, BrainProviders.byId("custom"))
        assertTrue(BrainProviders.cloud.none { it.custom })
    }

    @Test
    fun `the first model offered on a gateway is a small chat one`() {
        val list = listOf("flux-3", "nemotron-asr-multilingual", "gpt-5.6-luna", "claude-haiku-5.5")
        val chat = BrainProviders.chatModels(list)
        assertEquals("gpt-5.6-luna", BrainProviders.pickDefault(BrainProviders.Custom, chat))
    }
}
