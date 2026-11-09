package ru.gigapisar.brain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A live check of the whole "own server" chain from the phone: the address the person types,
 * the key, the model list of the service and a one-word probe request. The key comes from the
 * instrumentation arguments (GIGA_PISAR_TEST_KEY); without it the test is skipped, not failed.
 */
@RunWith(AndroidJUnit4::class)
class BrainCustomNetworkTest {
    private val key: String
        get() = InstrumentationRegistry.getArguments().getString("gigaPisarTestKey").orEmpty()

    @Test
    fun theAddressOfOurOwnServerIsReachedAndTheKeyIsChecked() {
        val server = requireNotNull(BrainProviders.customAt("https://plusvibeapi.ru/v1")) { "адрес не принят" }
        val fakeKey = "sk-pv-0000000000000000000000000000000000"
        val real = runCatching { BrainClient.listModels(server, fakeKey) }
        assertTrue("сервер отдал список моделей с заведомо неверным ключом", real.isFailure)
        val realMessage = real.exceptionOrNull()?.message.orEmpty()
        assertTrue("пустое сообщение об ошибке", realMessage.isNotBlank())

        // A host that does not exist must fail in another way: our address is really reached.
        val nobody = requireNotNull(BrainProviders.customAt("https://no-such-service-4242.invalid/v1"))
        val nowhere = runCatching { BrainClient.listModels(nobody, fakeKey) }
        val nowhereMessage = nowhere.exceptionOrNull()?.message.orEmpty()
        assertNotEquals("сбой одинаковый — запрос до сервера не доходит", nowhereMessage, realMessage)
        assertTrue(
            "ожидалась ошибка ключа, получено: $realMessage",
            realMessage.contains("ключ", ignoreCase = true) || realMessage.contains("key", ignoreCase = true),
        )
    }

    @Test
    fun aRealKeyGivesModelListAndAProbe() {
        assumeTrue("нет ключа в аргументах инструментации", key.isNotBlank())
        val server = requireNotNull(BrainProviders.customAt("https://plusvibeapi.ru/v1")) { "адрес не принят" }
        val models = BrainProviders.chatModels(BrainClient.listModels(server, key))
        assertTrue("сервер не отдал ни одной чат-модели", models.isNotEmpty())
        val model = requireNotNull(BrainProviders.pickDefault(server, models)) { "модель не выбралась" }
        // Throws a BrainException when the key or the model is not accepted.
        BrainClient.probe(server, key, model)
    }
}
