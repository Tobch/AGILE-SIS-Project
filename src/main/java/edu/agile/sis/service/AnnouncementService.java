package edu.agile.sis.service;

import edu.agile.sis.dao.AnnouncementDAO;
import org.bson.Document;

import java.io.InputStream;
import java.util.List;

public class AnnouncementService {
    private final AnnouncementDAO dao = new AnnouncementDAO();
    private final FileService fileService = new FileService();

    public String createAnnouncement(String title, String body, String category, boolean pinned, InputStream imageStream, String imageName, String contentType, String createdBy) {
        Document doc = new Document("title", title)
                .append("body", body)
                .append("category", category)
                .append("createdBy", createdBy)
                .append("isPinned", pinned)
                .append("deleted", false); 

        // Handle pinned date
        if (pinned) {
            doc.append("pinnedAt", new java.util.Date());
        } else {
            doc.append("pinnedAt", null);
        }
        
        // (Existing image logic)...
        if (imageStream != null) {
            String fileId = fileService.upload(imageStream, imageName, contentType, new Document("uploadedBy", createdBy));
            doc.append("thumbnailFileId", fileId);
        }
        return dao.insert(doc);
    }

    public List<Document> listFeed(int skip, int limit, String category) {
        return dao.list(skip, limit, category, true);
    }

    public Document getAnnouncement(String id) { return dao.findById(id); }

    public boolean update(String id, Document updates) { return dao.update(id, updates); }
}
