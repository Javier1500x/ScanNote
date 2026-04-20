package com.scannote.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DocumentDao {
    @Insert
    void insert(DocumentEntry document);

    @Update
    void update(DocumentEntry document);

    @Delete
    void delete(DocumentEntry document);

    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    LiveData<List<DocumentEntry>> getAllDocuments();

    @Query("SELECT * FROM documents WHERE title LIKE :search OR content LIKE :search")
    LiveData<List<DocumentEntry>> searchDocuments(String search);

    @Query("SELECT * FROM documents WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<DocumentEntry>> getDocumentsByType(String type);
}
