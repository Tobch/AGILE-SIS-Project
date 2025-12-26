package edu.agile.sis.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.io.OutputStream;

public class FileService {
    private final GridFSBucket bucket;

    public FileService() {
        this.bucket = GridFSBuckets.create(DBConnection.getInstance().getDatabase(), "attachments");
    }

    public String upload(InputStream in, String filename, String contentType, Document metadata) {
        GridFSUploadOptions opts = new GridFSUploadOptions().metadata(metadata == null ? new Document() : metadata.append("contentType", contentType));
        ObjectId id = bucket.uploadFromStream(filename, in, opts);
        return id.toHexString();
    }

    public void download(String fileIdHex, OutputStream out) throws Exception {
        ObjectId id = new ObjectId(fileIdHex);
        bucket.downloadToStream(id, out);
    }

    public void delete(String fileIdHex) {
        try { bucket.delete(new ObjectId(fileIdHex)); } catch (Exception ignored) {}
    }
}
