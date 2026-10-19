package com.example.data

import androidx.room.*
import com.example.model.MyPdf
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdfs ORDER BY dateCreated DESC")
    fun getAllPdfs(): Flow<List<MyPdf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: MyPdf)

    @Delete
    suspend fun delete(pdf: MyPdf)

    @Query("DELETE FROM pdfs WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
