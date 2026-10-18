package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ConversionResult
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaPurple
import com.example.util.FileUtils

@Composable
fun ResultSection(
    result: ConversionResult,
    onConvertMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPageIndex by remember { mutableStateOf(0) }

    // SAF Document Creator launcher for custom save
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val success = FileUtils.savePdfToCustomUri(context, result.file, uri)
            if (success) {
                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("result_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Header Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NovaCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = NovaCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PDF Generated Successfully!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${result.fileName} • ${result.pageCount} ${if (result.pageCount == 1) "page" else "pages"} • ${result.formattedSize}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Preview Section
        if (result.previewBitmaps.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PDF Page Preview",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tap to enlarge",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(result.previewBitmaps) { index, bitmap ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(210.dp)
                                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                        .shadow(4.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPreviewBitmap = bitmap
                                            selectedPageIndex = index
                                        }
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1} Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )

                                    // Zoom icon overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Enlarge",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Page ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Primary Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Download / Save to Device
            Button(
                onClick = {
                    val uri = FileUtils.savePdfToDownloads(context, result.file, result.fileName)
                    if (uri != null) {
                        Toast.makeText(context, "Saved to Downloads/NovaPDF!", Toast.LENGTH_LONG).show()
                    } else {
                        // Launch SAF picker if direct MediaStore failed
                        createDocumentLauncher.launch(result.fileName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_pdf_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save to Downloads",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Share & Open in row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { FileUtils.openPdf(context, result.file) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Open")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open File",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = { FileUtils.sharePdf(context, result.file) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // "Convert More" Reset Button
            Button(
                onClick = onConvertMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("convert_more_reset_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Convert more")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Convert More Images", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    // Full screen preview modal
    if (selectedPreviewBitmap != null) {
        Dialog(
            onDismissRequest = { selectedPreviewBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${selectedPageIndex + 1} of ${result.pageCount}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { selectedPreviewBitmap = null }
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        selectedPreviewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Enlarged Page ${selectedPageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}
