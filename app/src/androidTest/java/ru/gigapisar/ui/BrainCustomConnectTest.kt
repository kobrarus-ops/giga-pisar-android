package ru.gigapisar.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.gigapisar.R
import ru.gigapisar.brain.BrainProviders
import ru.gigapisar.brain.KeyVault
import ru.gigapisar.settings.SettingsRepository

/**
 * The whole path a person walks on the "own server" tab: type the address and the key, press
 * "Проверить и подключить", see the answer of the service and find the service saved, its key
 * in the Keystore and the model chosen. The key comes in the instrumentation arguments
 * (`gigaPisarTestKey`); without it the test is skipped, not failed.
 */
@RunWith(AndroidJUnit4::class)
class BrainCustomConnectTest {
    @get:Rule
    val compose = createComposeRule()

    private val key: String
        get() = InstrumentationRegistry.getArguments().getString("gigaPisarTestKey").orEmpty()

    private fun str(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun brain() = runBlocking { SettingsRepository.brain(context).first() }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theButtonConnectsTheServiceAndTheAnswerIsSaved() {
        assumeTrue("нет ключа в аргументах инструментации", key.isNotBlank())
        // Start from a clean slate so the check proves this run, not an earlier one.
        KeyVault.clear(context, BrainProviders.Custom.id)
        runBlocking {
            SettingsRepository.setBrainEnabled(context, false)
            SettingsRepository.setBrainService(context, null, null)
        }

        compose.setContent {
            val brain by SettingsRepository.brain(context).collectAsState(initial = SettingsRepository.BrainSettings())
            BrainScreen(brain = brain, onBack = {})
        }
        compose.onNodeWithText(str(R.string.brain_tab_custom)).performClick()

        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("https://plusvibeapi.ru/v1")
        fields[1].performTextReplacement(key)
        compose.onNodeWithText(str(R.string.brain_custom_connect)).performScrollTo().performClick()

        // The request goes out over the network: wait for it, then judge.
        val began = System.currentTimeMillis()
        var saved = brain()
        while (System.currentTimeMillis() - began < 60_000 && saved.providerId != BrainProviders.Custom.id) {
            Thread.sleep(500)
            saved = brain()
        }
        compose.waitForIdle()

        assertEquals("свой сервер не сохранился", BrainProviders.Custom.id, saved.providerId)
        assertEquals("адрес не сохранился", "https://plusvibeapi.ru/v1", saved.customUrl)
        assertNotNull("модель не выбралась", saved.model)
        assertTrue("обработка не включилась", saved.enabled)
        assertEquals("ключ не лёг в Keystore", key, KeyVault.load(context, BrainProviders.Custom.id))

        val tree = compose.onRoot().printToString(maxDepth = 100)
        assertTrue(
            "на экране нет ответа сервера, дерево: $tree",
            tree.contains("Пробный запрос прошёл") || tree.contains("plusvibeapi.ru"),
        )

        val file = File(context.filesDir, "own_server_connected.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }

        // A second picture from the top: the tabs and the answer of the service.
        compose.onNodeWithText("Пробный запрос прошёл", substring = true).performScrollTo()
        compose.waitForIdle()
        val top = File(context.filesDir, "own_server_connected_top.png")
        top.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
