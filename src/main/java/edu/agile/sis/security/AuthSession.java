package edu.agile.sis.security;

import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AuthSession {
    private static final AuthSession INSTANCE = new AuthSession();
    private Document currentUser = null;

    private AuthSession(){}

    public static AuthSession getInstance(){ return INSTANCE; }

    public void setCurrentUser(Document user){
        this.currentUser = user;
    }

    public Optional<Document> getCurrentUser(){
        return Optional.ofNullable(currentUser);
    }

    public String getUsername(){
        return currentUser == null ? null : currentUser.getString("username");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(){
        if (currentUser == null) return Collections.emptyList();
        List<String> roles = (List<String>) currentUser.get("roles");
        return roles == null ? Collections.emptyList() : roles;
    }

    public boolean hasRole(String role){
        return getRoles().stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public String getLinkedEntityId(){
        if (currentUser == null) return null;
        return currentUser.getString("linkedEntityId");
    }

    public void clear(){
        currentUser = null;
    }
}
