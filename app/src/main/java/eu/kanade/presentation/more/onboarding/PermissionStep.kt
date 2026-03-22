package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission

internal class PermissionStep : OnboardingStep {

    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val installGranted = rememberRequestPackageInstallsPermissionState()

        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    batteryGranted = context.getSystemService<PowerManager>()!!
                        .isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val permissionRequester = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {},
            )
        } else {
            null
        }

        PermissionStepContent(
            installGranted = installGranted,
            batteryGranted = batteryGranted,
            notificationGranted = notificationGranted,
            showNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            onInstallExtensions = { context.launchRequestPackageInstallsPermission() },
            onBattery = {
                @SuppressLint("BatteryLife")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            },
            onNotifications = {
                permissionRequester?.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
    }
}

@Composable
internal fun PermissionStepContent(
    installGranted: Boolean = false,
    batteryGranted: Boolean = false,
    notificationGranted: Boolean = false,
    showNotifications: Boolean = true,
    onInstallExtensions: () -> Unit = {},
    onBattery: () -> Unit = {},
    onNotifications: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = OnboardBlack),
    ) {
        // Background glow orbs
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = (-39).dp, y = (-88.3).dp)
                .requiredWidth(width = 195.dp)
                .requiredHeight(height = 442.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 120.dp)
                .background(color = OnboardBlue.copy(alpha = 0.05f)),
        )
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .offset(x = 39.dp, y = 44.dp)
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

            // ---- Shield / mascot icon ----
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(OnboardBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 40.dp)
                        .background(OnboardBlue.copy(alpha = 0.1f)),
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_onboard_shield),
                    contentDescription = null,
                    tint = OnboardBlue,
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---- Title ----
            Text(
                text = "Grant Permissions",
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
                text = "To provide the best reading experience,\nSora needs access to the following.",
                color = OnboardGray,
                textAlign = TextAlign.Center,
                lineHeight = 1.63.em,
                style = TextStyle(fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ---- Permission cards ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionCard(
                    icon = Icons.Outlined.Extension,
                    title = "Install Extensions",
                    subtitle = "Allow installation of manga sources",
                    granted = installGranted,
                    onToggle = { onInstallExtensions() },
                )
                PermissionCard(
                    icon = Icons.Outlined.BatteryAlert,
                    title = "Background Usage",
                    subtitle = "Keep library updated in background",
                    granted = batteryGranted,
                    onToggle = { onBattery() },
                )
                if (showNotifications) {
                    PermissionCard(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Get alerted when chapters release",
                        granted = notificationGranted,
                        onToggle = { onNotifications() },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(OnboardCard)
            .border(
                border = BorderStroke(
                    1.dp,
                    if (granted) OnboardBlue.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (granted) OnboardBlue else Color(0xFF262626)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) Color.Black else OnboardGray,
                modifier = Modifier.size(24.dp),
            )
        }
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                lineHeight = 1.5.em,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = subtitle,
                color = OnboardGray,
                lineHeight = 1.43.em,
                style = TextStyle(fontSize = 13.sp),
            )
        }
        // Toggle
        Switch(
            checked = granted,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OnboardBlue,
                uncheckedThumbColor = Color(0xFF9CA3AF),
                uncheckedTrackColor = Color(0xFF374151),
            ),
        )
    }
}
