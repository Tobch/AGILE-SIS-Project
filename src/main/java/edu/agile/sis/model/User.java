package edu.agile.sis.model;

import java.util.Date;
import java.util.List;

public class User {
    private String username;
    private String passwordHash;
    private List<String> roles;
    private String linkedEntityId;
    private Date createdAt;

    public User(String username, String passwordHash, List<String> roles, String linkedEntityId){
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.linkedEntityId = linkedEntityId;
        this.createdAt = new Date();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getLinkedEntityId() { return linkedEntityId; }
    public void setLinkedEntityId(String linkedEntityId) { this.linkedEntityId = linkedEntityId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
