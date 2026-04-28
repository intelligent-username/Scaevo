package com.scaevo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scaevo.ui.utils.formatDuration

@Composable
fun AppLimitProgressBar(
    usedMs: Long,
    limitMinutes: Int,
    modifier: Modifier = Modifier
) {
    val limitMs = limitMinutes * 60_000L
    val exceeded = limitMs <= 0L || usedMs >= limitMs
    val progress = if (limitMs <= 0L) 1f
    else (usedMs.toFloat() / limitMs.toFloat()).coerceIn(0f, 1f)
    val color = if (exceeded) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatDuration(usedMs)} used",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${limitMinutes}m limit",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
