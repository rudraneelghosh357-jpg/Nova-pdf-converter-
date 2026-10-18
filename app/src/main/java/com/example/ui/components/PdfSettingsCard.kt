package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImageQuality
import com.example.model.PageMargin
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfSettings
import com.example.ui.theme.NovaCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSettingsCard(
    settings: PdfSettings,
    onPageSizeChange: (PageSizeOption) -> Unit,
    onOrientationChange: (PageOrientation) -> Unit,
    onMarginChange: (PageMargin) -> Unit,
    onQualityChange: (ImageQuality) -> Unit,
    onFileNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_settings_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PDF Output Settings",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Page Size
            SettingRow(title = "Page Size") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageSizeOption.entries.forEach { option ->
                        FilterChip(
                            selected = settings.pageSize == option,
                            onClick = { onPageSizeChange(option) },
                            label = {
                                Text(
                                    text = option.label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("page_size_${option.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Orientation
            SettingRow(title = "Orientation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageOrientation.entries.forEach { orientation ->
                        FilterChip(
                            selected = settings.orientation == orientation,
                            onClick = { onOrientationChange(orientation) },
                            label = {
                                Text(
                                    text = orientation.label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("orientation_${orientation.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Margin
            SettingRow(title = "Page Margins") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageMargin.entries.forEach { margin ->
                        FilterChip(
                            selected = settings.margin == margin,
                            onClick = { onMarginChange(margin) },
                            label = {
                                Text(
                                    text = margin.label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("margin_${margin.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Quality (Compression)
            SettingRow(title = "Image Quality & Compression") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = settings.quality == quality,
                            onClick = { onQualityChange(quality) },
                            label = {
                                Text(
                                    text = quality.label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quality_${quality.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Custom File Name
            SettingRow(title = "Custom PDF File Name") {
                OutlinedTextField(
                    value = settings.customFileName,
                    onValueChange = onFileNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_file_name_input"),
                    placeholder = { Text("e.g. My_Scanned_Document", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = ".pdf",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mandatory Footer Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NovaCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Footer \"Created by Nova PDF Converter\" will be placed in the bottom-right corner of each page without overlapping images.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(5.dp))
        content()
    }
}
