package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Design tokens (shared across steps)
internal val OnboardBlack = Color(0xFF0E0E0E)
internal val OnboardBlue = Color(0xFF8AACFF)
internal val OnboardBlueDeep = Color(0xFF146DF5)
internal val OnboardCard = Color(0xFF1F1F1F)
internal val OnboardGray = Color(0xFFABABAB)

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        StorageStepContent(
            isComplete = _isComplete,
            storageLocationText = SettingsDataScreen.storageLocationText(storagePref),
            onSelectFolder = {
                try {
                    pickStorageLocation.launch(null)
                } catch (e: ActivityNotFoundException) {
                    context.toast(MR.strings.file_picker_error)
                }
            },
        )

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}

@Composable
internal fun StorageStepContent(
    isComplete: Boolean,
    storageLocationText: String = "Select Folder",
    onSelectFolder: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = OnboardBlack),
    ) {
        // ---- Background glow orbs ----
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .offset(x = 39.dp, y = (-88.39).dp)
                .requiredWidth(width = 195.dp)
                .requiredHeight(height = 442.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 120.dp)
                .background(color = OnboardBlue.copy(alpha = 0.05f)),
        )
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .offset(x = (-19.5).dp, y = 44.18.dp)
                .requiredWidth(width = 156.dp)
                .requiredHeight(height = 354.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 100.dp)
                .background(color = OnboardBlueDeep.copy(alpha = 0.05f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.35f))

            // ---- Folder icon box ----
            Box(
                modifier = Modifier
                    .requiredSize(size = 120.dp)
                    .clip(shape = RoundedCornerShape(24.dp))
                    .background(color = OnboardBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                // inner glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(9999.dp))
                        .blur(radius = 48.dp)
                        .background(color = OnboardBlue.copy(alpha = 0.1f)),
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_onboard_folder),
                    contentDescription = null,
                    tint = OnboardBlue,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Title ----
            Text(
                text = "Select Storage Folder",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.25.em,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Subtitle ----
            Text(
                text = "Choose a location to store your\ndownloaded manga chapters and data.",
                color = OnboardGray,
                textAlign = TextAlign.Center,
                lineHeight = 1.63.em,
                style = TextStyle(fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ---- Select Folder Card (highlighted) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(OnboardCard)
                    .border(BorderStroke(2.dp, OnboardBlue.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                    .clickable { onSelectFolder() },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(all = 20.dp),
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .requiredSize(size = 48.dp)
                            .clip(shape = CircleShape)
                            .background(color = OnboardBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_onboard_folder),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    // Text block
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isComplete) storageLocationText else "Select Folder",
                            color = Color.White,
                            lineHeight = 1.5.em,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                        Text(
                            text = "LOCAL DEVICE",
                            color = OnboardBlue,
                            lineHeight = 1.5.em,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.28.sp,
                            ),
                        )
                    }
                    // Recommended badge
                    Box(
                        modifier = Modifier
                            .clip(shape = CircleShape)
                            .background(color = OnboardBlue.copy(alpha = 0.2f))
                            .border(
                                border = BorderStroke(1.dp, OnboardBlue.copy(alpha = 0.3f)),
                                shape = CircleShape,
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            color = OnboardBlue,
                            lineHeight = 1.5.em,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Cloud Storage Card (unavailable / decorative) ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(20.dp))
                    .background(color = Color(0xFF191919).copy(alpha = 0.5f))
                    .border(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(all = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(size = 48.dp)
                        .clip(shape = CircleShape)
                        .background(color = Color(0xFF262626)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_onboard_cloud),
                        contentDescription = null,
                        tint = OnboardGray,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Storage",
                        color = OnboardGray,
                        lineHeight = 1.5.em,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "UNAVAILABLE",
                        color = OnboardGray.copy(alpha = 0.6f),
                        lineHeight = 1.5.em,
                        style = TextStyle(fontSize = 11.sp, letterSpacing = 0.28.sp),
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_onboard_lock),
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
