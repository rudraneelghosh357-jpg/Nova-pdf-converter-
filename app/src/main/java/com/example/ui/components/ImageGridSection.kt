package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.ImageItem
import com.example.ui.theme.NovaPdfRed

@Composable
fun ImageGridSection(
    images: List<ImageItem>,
    onRotateImage: (String) -> Unit,
    onCropImage: (String) -> Unit,
    onFilterImage: (String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeleteImage: (String) -> Unit,
    onClearAll: () -> Unit,
    onReverseOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with count and batch controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Selected Images",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${images.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Row {
                if (images.size > 1) {
                    TextButton(
                        onClick = onReverseOrder,
                        modifier = Modifier.testTag("reverse_order_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Reverse",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reverse", style = MaterialTheme.typography.labelSmall)
                    }
                }

                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = NovaPdfRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Clear",
                        color = NovaPdfRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Thumbnail list with reordering, rotation, crop and deletion
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            images.forEachIndexed { index, item ->
                ImageItemCard(
                    item = item,
                    pageIndex = index,
                    totalCount = images.size,
                    onRotate = { onRotateImage(item.id) },
                    onCrop = { onCropImage(item.id) },
                    onFilter = { onFilterImage(item.id) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onDelete = { onDeleteImage(item.id) }
                )
            }
        }
    }
}

@Composable
fun ImageItemCard(
    item: ImageItem,
    pageIndex: Int,
    totalCount: Int,
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onFilter: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("image_item_card_$pageIndex"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail 90dp
                Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize().rotate(item.rotationDegrees.toFloat()),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = if (item.formattedSize.isNotBlank()) item.formattedSize else "Image", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Action Buttons: 2 rows of 3 buttons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionButton(onClick = onRotate, icon = Icons.Default.RotateRight, description = "Rotate", modifier = Modifier.weight(1f))
                    ActionButton(onClick = onCrop, icon = Icons.Default.Crop, description = "Crop", modifier = Modifier.weight(1f))
                    ActionButton(onClick = onFilter, icon = Icons.Default.FilterList, description = "Filter", modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionButton(onClick = onMoveUp, icon = Icons.Default.ArrowUpward, description = "Up", modifier = Modifier.weight(1f), enabled = pageIndex > 0)
                    ActionButton(onClick = onMoveDown, icon = Icons.Default.ArrowDownward, description = "Down", modifier = Modifier.weight(1f), enabled = pageIndex < totalCount - 1)
                    ActionButton(onClick = onDelete, icon = Icons.Default.Delete, description = "Delete", modifier = Modifier.weight(1f), isDelete = true)
                }
            }
        }
    }
}

@Composable
fun ActionButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, modifier: Modifier, enabled: Boolean = true, isDelete: Boolean = false) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        colors = if (isDelete) IconButtonDefaults.filledTonalIconButtonColors(containerColor = NovaPdfRed.copy(alpha = 0.2f), contentColor = NovaPdfRed) else IconButtonDefaults.filledTonalIconButtonColors()
    ) {
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}
    }
}
