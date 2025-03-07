package com.vitorpamplona.amethyst.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vitorpamplona.amethyst.model.UrlCachedPreviewer
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.HalfVertPadding
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LoadUrlPreview(url: String, urlText: String, accountViewModel: AccountViewModel) {
    val automaticallyShowUrlPreview = remember {
        accountViewModel.settings.showUrlPreview.value
    }

    if (!automaticallyShowUrlPreview) {
        ClickableUrl(urlText, url)
    } else {
        var urlPreviewState by remember(url) {
            mutableStateOf(
                UrlCachedPreviewer.cache.get(url) ?: UrlPreviewState.Loading
            )
        }

        // Doesn't use a viewModel because of viewModel reusing issues (too many UrlPreview are created).
        if (urlPreviewState == UrlPreviewState.Loading) {
            LaunchedEffect(url) {
                accountViewModel.urlPreview(url) {
                    urlPreviewState = it
                }
            }
        }

        Crossfade(
            targetState = urlPreviewState,
            animationSpec = tween(durationMillis = 100),
            label = "UrlPreview"
        ) { state ->
            when (state) {
                is UrlPreviewState.Loaded -> {
                    if (state.previewInfo.mimeType.type == "image") {
                        Box(modifier = HalfVertPadding) {
                            ZoomableContentView(ZoomableUrlImage(url), persistentListOf(), roundedCorner = true, accountViewModel)
                        }
                    } else if (state.previewInfo.mimeType.type == "video") {
                        Box(modifier = HalfVertPadding) {
                            ZoomableContentView(ZoomableUrlVideo(url), persistentListOf(), roundedCorner = true, accountViewModel)
                        }
                    } else {
                        UrlPreviewCard(url, state.previewInfo)
                    }
                }

                else -> {
                    ClickableUrl(urlText, url)
                }
            }
        }
    }
}
