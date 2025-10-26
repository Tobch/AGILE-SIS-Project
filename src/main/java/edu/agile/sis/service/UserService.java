package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

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
}
