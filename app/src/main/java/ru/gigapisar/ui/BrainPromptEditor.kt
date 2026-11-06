package ru.gigapisar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.gigapisar.R
import ru.gigapisar.brain.Brain
import ru.gigapisar.settings.SettingsRepository

private fun title(kind: Brain.Kind): Int =
    when (kind) {
        Brain.Kind.COMMAND -> R.string.brain_prompt_commands
        Brain.Kind.TIDY -> R.string.brain_style_tidy
        Brain.Kind.SHORTEN -> R.string.brain_style_short
    }

/** What Pisar itself adds after the instructions; shown so that nothing goes to the model unseen. */
private fun addition(kind: Brain.Kind): Int? =
    when (kind) {
        Brain.Kind.COMMAND -> R.string.brain_prompt_adds_command
        Brain.Kind.TIDY -> null
        Brain.Kind.SHORTEN -> R.string.brain_prompt_adds_length
    }

/** "Instructions": one row per option of the Brain, each opens its editor. */
@Composable
internal fun BrainPromptRows(
    prompts: Brain.Custom,
    onOpen: (Brain.Kind) -> Unit,
) {
    SectionTitle(R.string.brain_prompts)
    for (kind in Brain.Kind.entries) {
        ListItem(
            headlineContent = { Text(stringResource(title(kind))) },
            supportingContent = {
                Text(stringResource(if (prompts.of(kind) == null) R.string.brain_prompt_standard else R.string.brain_prompt_own))
            },
            leadingContent = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.clickable { onOpen(kind) },
        )
    }
}

/**
 * The instructions of one option. The core rules on top and at the bottom are locked: with
 * them gone the model starts answering the take instead of editing it. The middle is the
 * person's to rewrite, and the standard text is one tap away.
 */
@Composable
internal fun BrainPromptEditor(
    kind: Brain.Kind,
    custom: String?,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val standard = Brain.instructions(kind)
    var text by rememberSaveable(kind) { mutableStateOf(custom ?: standard.body) }
    val changed = text.trim() != standard.body.trim()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.brain_prompt_title, stringResource(title(kind))),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Locked(standard.head)
            Text(
                stringResource(R.string.brain_prompt_yours),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(SettingsRepository.BRAIN_PROMPT_MAX) },
                textStyle = MaterialTheme.typography.bodyMedium,
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            Locked(
                standard.tail + (addition(kind)?.let { "\n\n" + stringResource(it) } ?: ""),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextButton(onClick = { text = standard.body }, enabled = changed) {
                Text(stringResource(R.string.brain_prompt_reset))
            }
            Button(
                onClick = {
                    scope.launch {
                        SettingsRepository.setBrainPrompt(context, kind, text)
                        onClose()
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.brain_prompt_save))
            }
        }
    }
}

/** A part of the instructions that stays as it is, with a lock and a caption saying so. */
@Composable
private fun Locked(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(
                    stringResource(R.string.brain_prompt_locked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
