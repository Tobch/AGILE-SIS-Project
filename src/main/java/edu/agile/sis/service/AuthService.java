package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public boolean register(String username, String password, List<String> roles, String linkedEntityId) {
        if (userDAO.findByUsername(username) != null) {
            return false; // user already exists
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        userDAO.insertUser(username, hash, roles, linkedEntityId);
        return true;
    }

   public boolean login(String username, String password) {
    Document user = userDAO.findByUsername(username);
    if (user == null) return false;

    String storedHash = user.getString("passwordHash");

    // Case 1: stored as bcrypt
    if (storedHash.startsWith("$2a$")) {
        return BCrypt.checkpw(password, storedHash);
    }

    // Case 2: old plain-text password
    if (storedHash.equals(password)) {
        // upgrade account by rehashing
        String newHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        userDAO.updatePassword(username, newHash);
        return true;
    }

    return false;
}


    public Document getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    public void changePassword(String username, String newPassword) {
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        userDAO.updatePassword(username, hash);
    }

    public List<Document> listAllUsers() {
        return userDAO.findAllUsers();
    }

    public void deleteUser(String username) {
        userDAO.deleteUser(username);
    }
}
