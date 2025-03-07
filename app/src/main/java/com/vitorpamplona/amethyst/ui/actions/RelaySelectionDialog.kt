package com.vitorpamplona.amethyst.ui.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.RelayBriefInfoCache
import com.vitorpamplona.amethyst.model.RelayInformation
import com.vitorpamplona.amethyst.service.Nip11Retriever
import com.vitorpamplona.amethyst.service.relays.Relay
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.FeedPadding
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class RelayList(
    val relay: Relay,
    val relayInfo: RelayBriefInfoCache.RelayBriefInfo,
    val isSelected: Boolean
)

data class RelayInfoDialog(
    val relayBriefInfo: RelayBriefInfoCache.RelayBriefInfo,
    val relayInfo: RelayInformation
)

@Composable
fun RelaySelectionDialog(
    preSelectedList: ImmutableList<Relay>,
    onClose: () -> Unit,
    onPost: (list: ImmutableList<Relay>) -> Unit,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val context = LocalContext.current

    var relays by remember {
        mutableStateOf(
            accountViewModel.account.activeWriteRelays().map {
                RelayList(
                    relay = it,
                    relayInfo = RelayBriefInfoCache.RelayBriefInfo(it.url),
                    isSelected = preSelectedList.any { relay -> it.url == relay.url }
                )
            }
        )
    }

    val hasSelectedRelay by remember {
        derivedStateOf {
            relays.any { it.isSelected }
        }
    }

    var relayInfo: RelayInfoDialog? by remember { mutableStateOf(null) }

    relayInfo?.let {
        RelayInformationDialog(
            onClose = {
                relayInfo = null
            },
            relayInfo = it.relayInfo,
            relayBriefInfo = it.relayBriefInfo,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }

    var selected by remember {
        mutableStateOf(true)
    }

    Dialog(
        onDismissRequest = { onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp)

            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CloseButton(
                        onPress = {
                            onClose()
                        }
                    )

                    SaveButton(
                        onPost = {
                            val selectedRelays = relays.filter { it.isSelected }
                            onPost(selectedRelays.map { it.relay }.toImmutableList())
                            onClose()
                        },
                        isActive = hasSelectedRelay
                    )
                }

                RelaySwitch(
                    text = context.getString(R.string.select_deselect_all),
                    checked = selected,
                    onClick = {
                        selected = !selected
                        relays = relays.mapIndexed { _, item ->
                            item.copy(isSelected = selected)
                        }
                    }
                )

                LazyColumn(
                    contentPadding = FeedPadding
                ) {
                    itemsIndexed(
                        relays,
                        key = { _, item -> item.relay.url }
                    ) { index, item ->
                        RelaySwitch(
                            text = item.relayInfo.displayUrl,
                            checked = item.isSelected,
                            onClick = {
                                relays = relays.mapIndexed { j, item ->
                                    if (index == j) {
                                        item.copy(isSelected = !item.isSelected)
                                    } else {
                                        item
                                    }
                                }
                            },
                            onLongPress = {
                                accountViewModel.retrieveRelayDocument(
                                    item.relay.url,
                                    onInfo = {
                                        relayInfo = RelayInfoDialog(
                                            RelayBriefInfoCache.RelayBriefInfo(
                                                item.relay.url
                                            ),
                                            it
                                        )
                                    },
                                    onError = { url, errorCode, exceptionMessage ->
                                        val msg = when (errorCode) {
                                            Nip11Retriever.ErrorCode.FAIL_TO_ASSEMBLE_URL -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                            Nip11Retriever.ErrorCode.FAIL_TO_REACH_SERVER -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                            Nip11Retriever.ErrorCode.FAIL_TO_PARSE_RESULT -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                            Nip11Retriever.ErrorCode.FAIL_WITH_HTTP_STATUS -> context.getString(R.string.relay_information_document_error_assemble_url, url, exceptionMessage)
                                        }

                                        accountViewModel.toast(
                                            context.getString(R.string.unable_to_download_relay_document),
                                            msg
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RelaySwitch(text: String, checked: Boolean, onClick: () -> Unit, onLongPress: () -> Unit = { }) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                onClick()
            }
        )
    }
}
