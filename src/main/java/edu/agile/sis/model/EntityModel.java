package edu.agile.sis.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class EntityModel {
    private String entityId;
    private String type;
    private Map<String,Object> core;
    private List<Map<String,Object>> attributes;
    private Boolean deleted;
    private Date createdAt;
    private Date updatedAt;

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String,Object> getCore() { return core; }
    public void setCore(Map<String,Object> core) { this.core = core; }
    public List<Map<String,Object>> getAttributes() { return attributes; }
    public void setAttributes(List<Map<String,Object>> attributes) { this.attributes = attributes; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
