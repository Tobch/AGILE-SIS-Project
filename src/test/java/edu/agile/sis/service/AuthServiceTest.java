package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {
    @Mock
    private UserDAO mockUserDAO;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("users")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        authService = new AuthService();

        // Inject mock DAO using reflection (field name in AuthService is 'userDAO')
        try {
            java.lang.reflect.Field daoField = AuthService.class.getDeclaredField("userDAO");
            daoField.setAccessible(true);
            daoField.set(authService, mockUserDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock UserDAO", e);
        }
    }

    @Test
    @DisplayName("register - should register user successfully")
    void testRegisterSuccess() {
        String username = "john_doe";
        String password = "secure_password";
        List<String> roles = Arrays.asList("STUDENT");
        String linkedEntityId = "entity-001";

        when(mockUserDAO.findByUsername(username)).thenReturn(null);

        boolean result = authService.register(username, password, roles, linkedEntityId);

        assertTrue(result);
        verify(mockUserDAO, times(1)).insertUser(eq(username), anyString(), eq(roles), eq(linkedEntityId));
    }

    @Test
    @DisplayName("register - should return false when user already exists")
    void testRegisterUserExists() {
        String username = "john_doe";
        Document existingUser = new Document("username", username);
        when(mockUserDAO.findByUsername(username)).thenReturn(existingUser);

        boolean result = authService.register(username, "password", Arrays.asList("STUDENT"), "entity-001");

        assertFalse(result);
        verify(mockUserDAO, times(0)).insertUser(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("login - should return true when credentials match")
    void testLoginSuccess() {
        String username = "john_doe";
        String password = "correct_password";
        // Generate a valid BCrypt hash for testing
        String passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        Document user = new Document("username", username).append("passwordHash", passwordHash);
        when(mockUserDAO.findByUsername(username)).thenReturn(user);

        boolean result = authService.login(username, password);

        assertTrue(result);
        verify(mockUserDAO, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("login - should return false when user not found")
    void testLoginUserNotFound() {
        when(mockUserDAO.findByUsername("unknown")).thenReturn(null);

        boolean result = authService.login("unknown", "password");

        assertFalse(result);
    }

    @Test
    @DisplayName("getUserByUsername - should return user document")
    void testGetUserByUsernameSuccess() {
        String username = "john_doe";
        Document user = new Document("username", username);
        when(mockUserDAO.findByUsername(username)).thenReturn(user);

        Document result = authService.getUserByUsername(username);

        assertEquals(user, result);
        verify(mockUserDAO, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("getUserByUsername - should return null when not found")
    void testGetUserByUsernameNotFound() {
        when(mockUserDAO.findByUsername("unknown")).thenReturn(null);

        Document result = authService.getUserByUsername("unknown");

        assertNull(result);
    }

    @Test
    @DisplayName("changePassword - should update password")
    void testChangePasswordSuccess() {
        String username = "john_doe";
        authService.changePassword(username, "new_password");

        verify(mockUserDAO, times(1)).updatePassword(eq(username), anyString());
    }

    @Test
    @DisplayName("listAllUsers - should return all users")
    void testListAllUsersSuccess() {
        List<Document> users = Arrays.asList(
            new Document("username", "user1"),
            new Document("username", "user2")
        );
        when(mockUserDAO.findAllUsers()).thenReturn(users);

        List<Document> result = authService.listAllUsers();

        assertEquals(2, result.size());
        verify(mockUserDAO, times(1)).findAllUsers();
    }

    @Test
    @DisplayName("deleteUser - should delete user by username")
    void testDeleteUserSuccess() {
        String username = "john_doe";
        authService.deleteUser(username);

        verify(mockUserDAO, times(1)).deleteUser(username);
    }
}
