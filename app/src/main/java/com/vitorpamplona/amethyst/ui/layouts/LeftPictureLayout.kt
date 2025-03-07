package com.vitorpamplona.amethyst.ui.layouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.note.LikeIcon
import com.vitorpamplona.amethyst.ui.note.TextCount
import com.vitorpamplona.amethyst.ui.note.ZappedIcon
import com.vitorpamplona.amethyst.ui.theme.DoubleHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.QuoteBorder
import com.vitorpamplona.amethyst.ui.theme.Size16Modifier
import com.vitorpamplona.amethyst.ui.theme.Size20Modifier
import com.vitorpamplona.amethyst.ui.theme.StdHorzSpacer
import com.vitorpamplona.amethyst.ui.theme.ThemeComparison
import com.vitorpamplona.amethyst.ui.theme.placeholderText

@Composable
@Preview
fun LeftPictureLayoutPreview() {
    ThemeComparison(
        onDark = { LeftPictureLayoutPreviewCard() },
        onLight = { LeftPictureLayoutPreviewCard() }
    )
}

@Composable
fun LeftPictureLayoutPreviewCard() {
    LeftPictureLayout(
        onImage = {
            Image(
                painter = painterResource(R.drawable.github),
                contentDescription = stringResource(id = R.string.profile_banner),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(QuoteBorder)
            )
        },
        onTitleRow = {
            Text(
                text = "This is my title",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = StdHorzSpacer)
            LikeIcon(
                iconSizeModifier = Size16Modifier,
                grayTint = MaterialTheme.colorScheme.onSurface
            )
            TextCount(12, MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = StdHorzSpacer)
            ZappedIcon(Size20Modifier)
            TextCount(120, MaterialTheme.colorScheme.onSurface)
        },
        onDescription = {
            Text(
                "This is 3-line description, This is 3-line description, This is 3-line description, This is 3-line description",
                color = MaterialTheme.colorScheme.placeholderText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        },
        onBottomRow = {
            Text("This is my Moderator List")
        }
    )
}

@Composable
fun LeftPictureLayout(
    onImage: @Composable () -> Unit,
    onTitleRow: @Composable RowScope.() -> Unit,
    onDescription: @Composable () -> Unit,
    onBottomRow: @Composable RowScope.() -> Unit
) {
    Row(Modifier.aspectRatio(ratio = 4f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .aspectRatio(ratio = 1f)
        ) {
            onImage()
        }

        Spacer(modifier = DoubleHorzSpacer)

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onTitleRow()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                onDescription()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                onBottomRow()
            }
        }
    }
}
