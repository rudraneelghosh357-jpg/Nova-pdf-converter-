package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdfs")
data class MyPdf(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uri: String,
    val dateCreated: Long,
    val sizeBytes: Long,
    val pageCount: Int
)
