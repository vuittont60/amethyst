package com.vitorpamplona.amethyst.ui.qrcode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.ui.actions.CloseButton
import com.vitorpamplona.amethyst.ui.components.CreateTextWithEmoji
import com.vitorpamplona.amethyst.ui.components.RobohashFallbackAsyncImage
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.quartz.events.UserMetadata
import com.vitorpamplona.quartz.events.toImmutableListOfLists

@Preview
@Composable
fun ShowQRDialogPreview() {
    val user = User("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")

    user.info = UserMetadata().apply {
        name = "My Name"
        picture = "Picture"
        banner = "http://banner.com/test"
        website = "http://mywebsite.com/test"
        about = "This is the about me"
    }

    ShowQRDialog(
        user = user,
        loadProfilePicture = false,
        onScan = {},
        onClose = {}
    )
}

@Composable
fun ShowQRDialog(user: User, loadProfilePicture: Boolean, onScan: (String) -> Unit, onClose: () -> Unit) {
    var presenting by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface {
            Column {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CloseButton(onPress = onClose)
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    if (presenting) {
                        Column() {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RobohashFallbackAsyncImage(
                                    robot = user.pubkeyHex,
                                    model = user.profilePicture(),
                                    contentDescription = stringResource(R.string.profile_image),
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(100.dp)
                                        .clip(shape = CircleShape)
                                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .background(MaterialTheme.colorScheme.background),
                                    loadProfilePicture = loadProfilePicture
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                            ) {
                                CreateTextWithEmoji(
                                    text = user.bestDisplayName() ?: user.bestUsername() ?: "",
                                    tags = user.info?.latestMetadata?.tags?.toImmutableListOfLists(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Size35dp)
                        ) {
                            QrCodeDrawer("nostr:${user.pubkeyNpub()}")
                        }

                        Row(modifier = Modifier.padding(horizontal = 30.dp)) {
                            Button(
                                onClick = { presenting = false },
                                shape = RoundedCornerShape(Size35dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults
                                    .buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                            ) {
                                Text(text = stringResource(R.string.scan_qr))
                            }
                        }
                    } else {
                        NIP19QrCodeScanner {
                            if (it.isNullOrEmpty()) {
                                presenting = true
                            } else {
                                onScan(it)
                            }
                        }
                    }
                }
            }
        }
    }
}
