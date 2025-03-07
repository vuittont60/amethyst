package com.vitorpamplona.amethyst.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.Material3RichText
import com.halilibo.richtext.ui.resolveDefaults
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.service.notifications.PushDistributorHandler
import com.vitorpamplona.amethyst.ui.screen.SharedPreferencesViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.CheckifItNeedsToRequestNotificationPermission
import com.vitorpamplona.amethyst.ui.screen.loggedIn.SettingsRow
import com.vitorpamplona.amethyst.ui.screen.loggedIn.SpinnerSelectionDialog
import com.vitorpamplona.amethyst.ui.screen.loggedIn.TitleExplainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SelectNotificationProvider(sharedPreferencesViewModel: SharedPreferencesViewModel) {
    val notificationPermissionState = CheckifItNeedsToRequestNotificationPermission(sharedPreferencesViewModel)

    if (notificationPermissionState.status.isGranted) {
        if (!sharedPreferencesViewModel.sharedPrefs.dontShowPushNotificationSelector) {
            val context = LocalContext.current
            var distributorPresent by remember {
                mutableStateOf(PushDistributorHandler.savedDistributorExists())
            }
            if (!distributorPresent) {
                LoadDistributors() { currentDistributor, list, readableListWithExplainer ->
                    if (readableListWithExplainer.size > 1) {
                        SpinnerSelectionDialog(
                            title = stringResource(id = R.string.select_push_server),
                            options = readableListWithExplainer,
                            onSelect = { index ->
                                if (list[index] == "None") {
                                    PushDistributorHandler.forceRemoveDistributor(context)
                                    sharedPreferencesViewModel.dontAskForNotificationPermissions()
                                    sharedPreferencesViewModel.dontShowPushNotificationSelector()
                                } else {
                                    val fullDistributorName = list[index]
                                    PushDistributorHandler.saveDistributor(fullDistributorName)
                                }
                                distributorPresent = true
                                Log.d("Amethyst", "NotificationScreen: Distributor registered.")
                            },
                            onDismiss = {
                                distributorPresent = true
                                Log.d("Amethyst", "NotificationScreen: Distributor dialog dismissed.")
                            }
                        )
                    } else {
                        AlertDialog(
                            onDismissRequest = {
                                distributorPresent = true
                            },
                            title = {
                                Text(stringResource(R.string.push_server_install_app))
                            },
                            text = {
                                Material3RichText(
                                    style = RichTextStyle().resolveDefaults()
                                ) {
                                    Markdown(
                                        content = stringResource(R.string.push_server_install_app_description)
                                    )
                                }
                            },
                            confirmButton = {
                                Row(
                                    modifier = Modifier
                                        .padding(all = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        distributorPresent = true
                                        sharedPreferencesViewModel.dontShowPushNotificationSelector()
                                    }) {
                                        Text(stringResource(R.string.quick_action_dont_show_again_button))
                                    }
                                    Button(onClick = {
                                        distributorPresent = true
                                    }, contentPadding = PaddingValues(horizontal = 16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.error_dialog_button_ok))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                val currentDistributor = PushDistributorHandler.getSavedDistributor()
                PushDistributorHandler.saveDistributor(currentDistributor)
            }
        }
    }
}

@Composable
fun LoadDistributors(
    onInner: @Composable (String, ImmutableList<String>, ImmutableList<TitleExplainer>) -> Unit
) {
    val currentDistributor = PushDistributorHandler.getSavedDistributor().ifBlank { null } ?: "None"

    val list = remember {
        PushDistributorHandler.getInstalledDistributors().plus("None").toImmutableList()
    }

    val readableListWithExplainer =
        PushDistributorHandler.formattedDistributorNames()
            .mapIndexed { index, name ->
                TitleExplainer(
                    name,
                    stringResource(id = R.string.push_server_uses_app_explainer, list[index])
                )
            }
            .plus(
                TitleExplainer(
                    stringResource(id = R.string.push_server_none),
                    stringResource(id = R.string.push_server_none_explainer)
                )
            )
            .toImmutableList()

    onInner(
        currentDistributor,
        list,
        readableListWithExplainer
    )
}

@Composable
fun PushNotificationSettingsRow(sharedPreferencesViewModel: SharedPreferencesViewModel) {
    val context = LocalContext.current

    LoadDistributors() { currentDistributor, list, readableListWithExplainer ->
        SettingsRow(
            R.string.push_server_title,
            R.string.push_server_explainer,
            selectedItens = readableListWithExplainer,
            selectedIndex = list.indexOf(currentDistributor)
        ) { index ->
            if (list[index] == "None") {
                sharedPreferencesViewModel.dontAskForNotificationPermissions()
                sharedPreferencesViewModel.dontShowPushNotificationSelector()
                PushDistributorHandler.forceRemoveDistributor(context)
            } else {
                PushDistributorHandler.saveDistributor(list[index])
            }
        }
    }
}
