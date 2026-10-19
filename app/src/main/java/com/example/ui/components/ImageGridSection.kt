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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with rotation preview & Page number tag
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(item.rotationDegrees.toFloat()),
                    contentScale = ContentScale.Crop
                )

                // Page badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Page ${pageIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Rotation degree indicator
                if (item.rotationDegrees != 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${item.rotationDegrees}°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (item.formattedSize.isNotBlank()) item.formattedSize else "Image",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons (Rotate, Crop, Reorder Up/Down, Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Rotate 90°
                FilledTonalIconButton(
                    onClick = onRotate,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("rotate_button_$pageIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate 90 degrees",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Crop
                FilledTonalIconButton(
                    onClick = onCrop,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("crop_button_$pageIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = "Crop image",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Filter
                FilledTonalIconButton(
                    onClick = onFilter,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("filter_button_$pageIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter image",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move Up
                FilledTonalIconButton(
                    onClick = onMoveUp,
                    enabled = pageIndex > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("move_up_button_$pageIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move Down
                FilledTonalIconButton(
                    onClick = onMoveDown,
                    enabled = pageIndex < totalCount - 1,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("move_down_button_$pageIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_$pageIndex"),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = NovaPdfRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete image",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
