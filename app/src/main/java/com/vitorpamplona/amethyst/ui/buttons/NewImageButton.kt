package com.vitorpamplona.amethyst.ui.buttons

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.actions.GallerySelect
import com.vitorpamplona.amethyst.ui.actions.NewMediaModel
import com.vitorpamplona.amethyst.ui.actions.NewMediaView
import com.vitorpamplona.amethyst.ui.navigation.Route
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.Size55Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NewImageButton(accountViewModel: AccountViewModel, nav: (String) -> Unit, navScrollToTop: (Route, Boolean) -> Unit) {
    var wantsToPost by remember {
        mutableStateOf(false)
    }

    var pickedURI by remember {
        mutableStateOf<Uri?>(null)
    }

    val scope = rememberCoroutineScope()

    val postViewModel: NewMediaModel = viewModel()
    postViewModel.onceUploaded {
        scope.launch(Dispatchers.Default) {
            delay(500)
            withContext(Dispatchers.Main) {
                navScrollToTop(Route.Video, true)
            }
        }
    }

    if (wantsToPost) {
        val cameraPermissionState =
            rememberPermissionState(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )

        if (cameraPermissionState.status.isGranted) {
            var showGallerySelect by remember { mutableStateOf(false) }
            if (showGallerySelect) {
                GallerySelect(
                    onImageUri = { uri ->
                        wantsToPost = false
                        showGallerySelect = false
                        pickedURI = uri
                    }
                )
            }

            showGallerySelect = true
        } else {
            LaunchedEffect(key1 = accountViewModel) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    pickedURI?.let {
        NewMediaView(
            uri = it,
            onClose = { pickedURI = null },
            postViewModel = postViewModel,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }

    if (postViewModel.isUploadingImage) {
        ShowProgress(postViewModel)
    } else {
        FloatingActionButton(
            onClick = { wantsToPost = true },
            modifier = Size55Modifier,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_compose),
                null,
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ShowProgress(postViewModel: NewMediaModel) {
    Box(Modifier.size(55.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = animateFloatAsState(
                targetValue = postViewModel.uploadingPercentage.value,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            ).value,
            modifier = Size55Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            strokeWidth = 5.dp
        )
        postViewModel.uploadingDescription.value?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
