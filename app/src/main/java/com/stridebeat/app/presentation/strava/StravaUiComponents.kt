package com.stridebeat.app.presentation.strava

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.presentation.theme.GlassPill
import com.stridebeat.app.presentation.theme.StatusError
import com.stridebeat.app.presentation.theme.StatusSuccess
import com.stridebeat.app.presentation.theme.StatusWarning
import com.stridebeat.app.presentation.theme.StravaBrand
import com.stridebeat.app.presentation.theme.StrideBeatGlass
import com.stridebeat.app.util.StravaFormatter

/**
 * Membuka aktivitas di aplikasi/web Strava, atau menyalin tautan ke clipboard sebagai fallback (US-14).
 */
fun openOrCopyStravaActivityLink(context: Context, activityId: String?) {
    if (activityId.isNullOrBlank()) return
    val url = StravaFormatter.buildStravaActivityUrl(activityId)
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Link Strava", url))
        Toast.makeText(context, "Link Strava disalin: $url", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun StravaStatusBadge(
    status: StravaUploadStatus,
    stravaActivityId: String?,
    onClick: (() -> Unit)? = null
) {
    val glass = StrideBeatGlass.colors
    val (label, tint, icon) = when (status) {
        StravaUploadStatus.UPLOADED -> Triple("Strava ✓", StravaBrand, Icons.Default.CheckCircle)
        StravaUploadStatus.PENDING -> Triple("Menunggu", StatusWarning, Icons.Default.Schedule)
        StravaUploadStatus.FAILED -> Triple("Gagal — coba lagi", StatusError, Icons.Default.ErrorOutline)
        StravaUploadStatus.NOT_UPLOADED -> Triple("Belum diunggah", glass.textMuted, Icons.Default.CloudOff)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    GlassPill(
        tint = tint,
        shape = RoundedCornerShape(8.dp),
        modifier = clickableModifier.semantics {
            contentDescription = "Status Strava: $label"
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ImportedFromStravaBadge() {
    GlassPill(
        tint = StravaBrand,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.semantics {
            contentDescription = "Sumber aktivitas: Dari Strava"
        }
    ) {
        Text(
            text = "Dari Strava",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = StravaBrand,
            fontWeight = FontWeight.Bold
        )
    }
}
