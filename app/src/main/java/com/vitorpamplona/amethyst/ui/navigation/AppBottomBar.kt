package com.vitorpamplona.amethyst.ui.navigation

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.BottomTopHeight
import com.vitorpamplona.amethyst.ui.theme.DividerThickness
import com.vitorpamplona.amethyst.ui.theme.Font12SP
import com.vitorpamplona.amethyst.ui.theme.Size0dp
import com.vitorpamplona.amethyst.ui.theme.Size10Modifier
import com.vitorpamplona.amethyst.ui.theme.Size10dp
import kotlinx.collections.immutable.persistentListOf

val bottomNavigationItems = persistentListOf(
    Route.Home,
    Route.Message,
    Route.Video,
    Route.Discover,
    Route.Notification
)

enum class Keyboard {
    Opened, Closed
}

fun isKeyboardOpen(view: View): Keyboard {
    val rect = Rect()
    view.getWindowVisibleDisplayFrame(rect)
    val screenHeight = view.rootView.height
    val keypadHeight = screenHeight - rect.bottom

    return if (keypadHeight > screenHeight * 0.15) {
        Keyboard.Opened
    } else {
        Keyboard.Closed
    }
}

@Composable
fun keyboardAsState(): State<Keyboard> {
    val view = LocalView.current

    val keyboardState = remember(view) {
        mutableStateOf(isKeyboardOpen(view))
    }

    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val newKeyboardValue = isKeyboardOpen(view)

            if (newKeyboardValue != keyboardState.value) {
                keyboardState.value = newKeyboardValue
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}

@Composable
fun IfKeyboardClosed(
    inner: @Composable () -> Unit
) {
    val isKeyboardState by keyboardAsState()
    if (isKeyboardState == Keyboard.Closed) {
        inner()
    }
}

@Composable
fun AppBottomBar(accountViewModel: AccountViewModel, navEntryState: State<NavBackStackEntry?>, nav: (Route, Boolean) -> Unit) {
    IfKeyboardClosed {
        RenderBottomMenu(accountViewModel, navEntryState, nav)
    }
}

@Composable
private fun RenderBottomMenu(
    accountViewModel: AccountViewModel,
    navEntryState: State<NavBackStackEntry?>,
    nav: (Route, Boolean) -> Unit
) {
    Column(modifier = BottomTopHeight) {
        Divider(
            thickness = DividerThickness
        )
        NavigationBar(tonalElevation = Size0dp) {
            bottomNavigationItems.forEach { item ->
                HasNewItemsIcon(item, accountViewModel, navEntryState, nav)
            }
        }
    }
}

@Composable
private fun RowScope.HasNewItemsIcon(
    route: Route,
    accountViewModel: AccountViewModel,
    navEntryState: State<NavBackStackEntry?>,
    nav: (Route, Boolean) -> Unit
) {
    val selected by remember(navEntryState.value) {
        derivedStateOf {
            navEntryState.value?.destination?.route?.substringBefore("?") == route.base
        }
    }

    NavigationBarItem(
        icon = {
            NotifiableIcon(
                selected,
                route,
                accountViewModel
            )
        },
        selected = selected,
        onClick = { nav(route, selected) }
    )
}

@Composable
private fun NotifiableIcon(
    selected: Boolean,
    route: Route,
    accountViewModel: AccountViewModel
) {
    Box(route.notifSize) {
        Icon(
            painter = painterResource(id = route.icon),
            contentDescription = null,
            modifier = route.iconSize,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified
        )

        AddNotifIconIfNeeded(route, accountViewModel, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun AddNotifIconIfNeeded(
    route: Route,
    accountViewModel: AccountViewModel,
    modifier: Modifier
) {
    val flow = accountViewModel.notificationDots.hasNewItems[route] ?: return
    val hasNewItems by flow.collectAsStateWithLifecycle()
    if (hasNewItems) {
        NotificationDotIcon(modifier)
    }
}

@Composable
private fun NotificationDotIcon(modifier: Modifier) {
    Box(modifier.size(Size10dp)) {
        Box(
            modifier = remember {
                Size10Modifier.clip(shape = CircleShape)
            }.background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                "",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = Font12SP,
                modifier = remember {
                    Modifier
                        .wrapContentHeight()
                        .align(Alignment.TopEnd)
                }
            )
        }
    }
}
