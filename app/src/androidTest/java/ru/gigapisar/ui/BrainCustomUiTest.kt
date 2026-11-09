package ru.gigapisar.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.gigapisar.R
import ru.gigapisar.settings.SettingsRepository
import java.io.File

/**
 * The "own server" tab on a real phone: it is reachable, it takes an address, and the
 * connect button stays off until the address can actually be used.
 */
@RunWith(AndroidJUnit4::class)
class BrainCustomUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun openOwnServerTab() {
        compose.setContent {
            BrainScreen(brain = SettingsRepository.BrainSettings(), onBack = {})
        }
        compose.onNodeWithText(str(R.string.brain_tab_custom)).performClick()
    }

    @Test
    fun theTabHasAnAddressFieldAKeyFieldAndAModelField() {
        openOwnServerTab()
        // Three fields on the tab: the address, the key and the model; the address comes first.
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].assertExists()
        fields[1].assertExists()
        fields[2].assertExists()
        compose.onNodeWithText(str(R.string.brain_custom_connect)).performScrollTo().assertExists()
    }

    @Test
    fun theConnectButtonWaitsForAnAddressThatCanBeUsed() {
        openOwnServerTab()
        val button = compose.onNodeWithText(str(R.string.brain_custom_connect))
        // Nothing typed yet: nothing to connect to.
        button.performScrollTo().assertIsNotEnabled()

        // Plain http is refused by the app, so the button must not offer it either.
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("http://plusvibeapi.ru/v1")
        button.performScrollTo().assertIsNotEnabled()

        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("https://plusvibeapi.ru/v1")
        button.performScrollTo().assertIsEnabled()
    }

    /** Pictures of the tab for the person who asked for it: the top with the tabs, then the fields. */
    @Test
    fun picturesOfTheTab() {
        openOwnServerTab()
        val filesDir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        save(compose.onRoot().captureToImage().asAndroidBitmap(), File(filesDir, "own_server_tab_top.png"))
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("https://plusvibeapi.ru/v1")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("sk-pv-образец-ключа")
        compose.onAllNodes(hasSetTextAction())[2].performTextReplacement("gpt-5.6-luna")
        compose.waitForIdle()
        save(compose.onRoot().captureToImage().asAndroidBitmap(), File(filesDir, "own_server_tab_fields.png"))
    }

    private fun save(
        bitmap: Bitmap,
        file: File,
    ) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
