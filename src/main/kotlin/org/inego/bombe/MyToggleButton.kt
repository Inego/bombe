package org.inego.bombe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun <T> MyToggleButton(
    options: List<T>,
    current: T,
    toggle: (T) -> Unit,
    render: @Composable (T, Boolean) -> Unit
) {
    Row {
        for (option in options) {
            val isCurrent = option == current
            Surface(
                color = if (isCurrent) MaterialTheme.colors.secondary else MaterialTheme.colors.background,
                border = BorderStroke(1.dp, Color.DarkGray),
                elevation = if (isCurrent) 0.dp else 5.dp,
                modifier = Modifier.clickable {
                    toggle(option)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    render(option, isCurrent)
                }
            }
        }
    }
}