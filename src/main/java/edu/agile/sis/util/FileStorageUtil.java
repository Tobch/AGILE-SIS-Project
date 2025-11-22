package edu.agile.sis.util;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.gridfs.model.GridFSFile;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;

public class FileStorageUtil {
    private static final MongoDatabase db = DBConnection.getInstance().getDatabase();
    private static final GridFSBucket bucket = GridFSBuckets.create(db); // default "fs"

    /**
     * Uploads the provided InputStream and returns the fileId (hex string).
     */
    public static String uploadFile(String filename, InputStream data, String contentType) throws IOException {
        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(new Document("contentType", contentType == null ? "application/octet-stream" : contentType));
        ObjectId id = bucket.uploadFromStream(filename, data, options);
        return id.toHexString();
    }

    /**
     * Download file to destination path
     */
    public static void downloadFile(String fileIdHex, String destPath) throws IOException {
        ObjectId id = new ObjectId(fileIdHex);
        try (OutputStream os = new FileOutputStream(destPath)) {
            bucket.downloadToStream(id, os);
        }
    }

    /**
     * Return InputStream of a file for previewing.
     */
    public static InputStream getFileStream(String fileIdHex) throws IOException {
        ObjectId id = new ObjectId(fileIdHex);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bucket.downloadToStream(id, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Delete file in GridFS by id.
     */
    public static void deleteFile(String fileIdHex) {
        ObjectId id = new ObjectId(fileIdHex);
        bucket.delete(id);
    }

    /**
     * Safe filename strip (very simple).
     */
    public static String safeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Get filename from GridFS metadata (optional)
     */
    public static String getFilename(String fileIdHex) {
        GridFSFile f = bucket.find(new Document("_id", new ObjectId(fileIdHex))).first();
        return f == null ? null : f.getFilename();
    }
}
