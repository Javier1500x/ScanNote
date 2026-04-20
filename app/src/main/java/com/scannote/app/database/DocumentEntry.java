package com.scannote.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String content;
    public long timestamp;
    public String type; // "Book", "Document", "Receipt", etc.
    public String folderName;

    public DocumentEntry(String title, String content, long timestamp, String type, String folderName) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
        this.folderName = folderName;
    }
}
