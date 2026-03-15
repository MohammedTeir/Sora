package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.presentation.theme.SoraBlue
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Illustration Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = SoraBlue,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title Section
            Text(
                text = "Select Storage Folder",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Choose a location to store your downloaded manga chapters and data.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Folder Selection Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        try {
                            pickStorageLocation.launch(null)
                        } catch (e: ActivityNotFoundException) {
                            context.toast(MR.strings.file_picker_error)
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = SoraBlue
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (_isComplete) SettingsDataScreen.storageLocationText(storagePref) else "Select Folder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = "RECOMMENDED",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Arrow
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
