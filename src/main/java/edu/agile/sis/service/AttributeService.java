package edu.agile.sis.service;

import edu.agile.sis.dao.AttributeMetaDAO;
import org.bson.Document;

import java.util.List;

public class AttributeService {
    private final AttributeMetaDAO dao = new AttributeMetaDAO();

    public void createAttributeMeta(Document meta){ dao.insert(meta); }

    public List<Document> listAll(){ return dao.findAll(); }

    public void update(String key, Document updated){ dao.update(key, updated); }

    public void delete(String key){ dao.delete(key); }

    public Document findByKey(String key){ return dao.findByKey(key); }
}
