package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    // Constructor for dependency injection (used in tests)
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Create user with bcrypt hash for password.
     * Returns false if username already exists.
     */
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

}
