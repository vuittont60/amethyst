package com.vitorpamplona.amethyst.ui.components

import android.content.res.Resources
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.service.lang.LanguageTranslatorService
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.lessImportantLink
import com.vitorpamplona.quartz.events.ImmutableListOfLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TranslatableRichTextViewer(
    content: String,
    canPreview: Boolean,
    modifier: Modifier = Modifier,
    tags: ImmutableListOfLists<String>,
    backgroundColor: MutableState<Color>,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var translatedTextState by remember(content) {
        mutableStateOf(TranslationConfig(content, null, null, false))
    }

    TranslateAndWatchLanguageChanges(content, accountViewModel) { result ->
        if (!translatedTextState.result.equals(result.result, true) ||
            translatedTextState.sourceLang != result.sourceLang ||
            translatedTextState.targetLang != result.targetLang
        ) {
            translatedTextState = result
        }
    }

    Crossfade(targetState = translatedTextState) {
        RenderText(
            it,
            content,
            canPreview,
            modifier,
            tags,
            backgroundColor,
            accountViewModel,
            nav
        )
    }
}

@Composable
private fun RenderText(
    translatedTextState: TranslationConfig,
    content: String,
    canPreview: Boolean,
    modifier: Modifier,
    tags: ImmutableListOfLists<String>,
    backgroundColor: MutableState<Color>,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var showOriginal by remember(translatedTextState) { mutableStateOf(translatedTextState.showOriginal) }

    val toBeViewed by remember(translatedTextState) {
        derivedStateOf {
            if (showOriginal) content else translatedTextState.result ?: content
        }
    }

    Column {
        ExpandableRichTextViewer(
            toBeViewed,
            canPreview,
            modifier,
            tags,
            backgroundColor,
            accountViewModel,
            nav
        )

        if (translatedTextState.sourceLang != null &&
            translatedTextState.targetLang != null &&
            translatedTextState.sourceLang != translatedTextState.targetLang
        ) {
            TranslationMessage(
                translatedTextState.sourceLang,
                translatedTextState.targetLang,
                accountViewModel
            ) {
                showOriginal = it
            }
        }
    }
}

@Composable
private fun TranslationMessage(
    source: String,
    target: String,
    accountViewModel: AccountViewModel,
    onChangeWhatToShow: (Boolean) -> Unit
) {
    var langSettingsPopupExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
    ) {
        val clickableTextStyle =
            SpanStyle(color = MaterialTheme.colorScheme.lessImportantLink)

        val annotatedTranslationString = buildAnnotatedString {
            withStyle(clickableTextStyle) {
                pushStringAnnotation("langSettings", true.toString())
                append(stringResource(R.string.translations_auto))
            }

            append("-${stringResource(R.string.translations_translated_from)} ")

            withStyle(clickableTextStyle) {
                pushStringAnnotation("showOriginal", true.toString())
                append(Locale(source).displayName)
            }

            append(" ${stringResource(R.string.translations_to)} ")

            withStyle(clickableTextStyle) {
                pushStringAnnotation("showOriginal", false.toString())
                append(Locale(target).displayName)
            }
        }

        ClickableText(
            text = annotatedTranslationString,
            style = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.32f
                )
            ),
            overflow = TextOverflow.Visible,
            maxLines = 3
        ) { spanOffset ->
            annotatedTranslationString.getStringAnnotations(spanOffset, spanOffset)
                .firstOrNull()
                ?.also { span ->
                    if (span.tag == "showOriginal") {
                        onChangeWhatToShow(span.item.toBoolean())
                    } else {
                        langSettingsPopupExpanded = !langSettingsPopupExpanded
                    }
                }
        }

        DropdownMenu(
            expanded = langSettingsPopupExpanded,
            onDismissRequest = { langSettingsPopupExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    if (source in accountViewModel.account.dontTranslateFrom) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        stringResource(
                            R.string.translations_never_translate_from_lang,
                            Locale(source).displayName
                        )
                    )
                },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.dontTranslateFrom(source)
                        langSettingsPopupExpanded = false
                    }
                }
            )
            Divider()
            DropdownMenuItem(
                text = {
                    if (accountViewModel.account.preferenceBetween(source, target) == source) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        stringResource(
                            R.string.translations_show_in_lang_first,
                            Locale(source).displayName
                        )
                    )
                },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.prefer(source, target, source)
                        langSettingsPopupExpanded = false
                    }
                }
            )
            DropdownMenuItem(
                text = {
                    if (accountViewModel.account.preferenceBetween(source, target) == target) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        stringResource(
                            R.string.translations_show_in_lang_first,
                            Locale(target).displayName
                        )
                    )
                },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.prefer(source, target, target)
                        langSettingsPopupExpanded = false
                    }
                }
            )
            Divider()

            val languageList =
                ConfigurationCompat.getLocales(Resources.getSystem().configuration)
            for (i in 0 until languageList.size()) {
                languageList.get(i)?.let { lang ->
                    DropdownMenuItem(
                        text = {
                            if (lang.language in accountViewModel.account.translateTo) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.size(10.dp))

                            Text(
                                stringResource(
                                    R.string.translations_always_translate_to_lang,
                                    lang.displayName
                                )
                            )
                        },
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                accountViewModel.translateTo(lang)
                                langSettingsPopupExpanded = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TranslateAndWatchLanguageChanges(content: String, accountViewModel: AccountViewModel, onTranslated: (TranslationConfig) -> Unit) {
    val accountState by accountViewModel.accountLanguagesLiveData.observeAsState()

    LaunchedEffect(accountState) {
        // This takes some time. Launches as a Composition scope to make sure this gets cancel if this item gets out of view.
        launch(Dispatchers.IO) {
            LanguageTranslatorService.autoTranslate(
                content,
                accountViewModel.account.dontTranslateFrom,
                accountViewModel.account.translateTo
            ).addOnCompleteListener { task ->
                if (task.isSuccessful && !content.equals(task.result.result, true)) {
                    if (task.result.sourceLang != null && task.result.targetLang != null) {
                        val preference = accountViewModel.account.preferenceBetween(task.result.sourceLang!!, task.result.targetLang!!)
                        val newConfig = TranslationConfig(
                            result = task.result.result,
                            sourceLang = task.result.sourceLang,
                            targetLang = task.result.targetLang,
                            showOriginal = preference == task.result.sourceLang
                        )

                        onTranslated(newConfig)
                    }
                }
            }
        }
    }
}
