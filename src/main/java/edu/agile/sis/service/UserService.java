package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }


    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

  
    public boolean createUser(String username, String password, List<String> roles, String linkedEntityId) {
        if (userDAO.findByUsername(username) != null) return false;
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
        userDAO.insertUser(username, hashed, roles, linkedEntityId);
        return true;
    }
    
    
    public boolean deleteUserByLinkedEntityId(String linkedEntityId) {
    if (linkedEntityId == null || linkedEntityId.isBlank()) return false;
    return userDAO.deleteByLinkedEntityId(linkedEntityId);
}
    
    
     public boolean updateUserByLinkedEntityId(String linkedEntityId, String newUsername, List<String> newRoles) {
        if (linkedEntityId == null || linkedEntityId.isBlank()) return false;
        try {
            return userDAO.updateUserByLinkedEntityId(linkedEntityId, newUsername, newRoles);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
     
     
     
     
     
public org.bson.Document getUserByEntityId(String entityId) {
    try {
        return userDAO.findByLinkedEntityId(entityId);
    } catch (Exception ex) {
        ex.printStackTrace();
        return null;
    }
}

public boolean updateUsernameForEntity(String entityId, String newUsername) {
    try {
        return userDAO.updateUserByLinkedEntityId(entityId, newUsername, null);
    } catch (Exception ex) {
        ex.printStackTrace();
        return false;
    }
}

public boolean updatePasswordForEntity(String entityId, String rawPassword) {
    try {
        org.bson.Document user = userDAO.findByLinkedEntityId(entityId);
        if (user == null) return false;
        String username = user.getString("username");
        if (username == null || username.isBlank()) return false;
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        userDAO.updatePassword(username, hashed);
        return true;
    } catch (Exception ex) {
        ex.printStackTrace();
        return false;
    }
}

public boolean deleteUserByEntityId(String entityId) {
    try {
        return userDAO.deleteByLinkedEntityId(entityId);
    } catch (Exception ex) {
        ex.printStackTrace();
        return false;
    }
}


}
