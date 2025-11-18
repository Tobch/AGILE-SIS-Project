package edu.agile.sis.service;

import edu.agile.sis.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {
    @Mock
    private UserDAO mockUserDAO;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Use constructor injection available on UserService to avoid DB access
        userService = new UserService(mockUserDAO);
    }

    @Test
    @DisplayName("createUser - should create user when username not taken")
    void testCreateUserSuccess() {
        String username = "alice";
        String password = "s3cret";
        List<String> roles = Arrays.asList("STUDENT");
        String linkedEntityId = "entity-123";

        when(mockUserDAO.findByUsername(username)).thenReturn(null);

        boolean result = userService.createUser(username, password, roles, linkedEntityId);

        assertTrue(result);
        verify(mockUserDAO, times(1)).insertUser(eq(username), anyString(), eq(roles), eq(linkedEntityId));
    }

    @Test
    @DisplayName("createUser - should return false when username exists")
    void testCreateUserExists() {
        String username = "alice";
        when(mockUserDAO.findByUsername(username)).thenReturn(new org.bson.Document("username", username));

        boolean result = userService.createUser(username, "pw", Arrays.asList("STUDENT"), "entity-001");

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteUserByLinkedEntityId - should delete when linkedEntityId valid")
    void testDeleteByLinkedEntityIdSuccess() {
        String linkedEntityId = "entity-456";
        when(mockUserDAO.deleteByLinkedEntityId(linkedEntityId)).thenReturn(true);

        boolean result = userService.deleteUserByLinkedEntityId(linkedEntityId);

        assertTrue(result);
        verify(mockUserDAO, times(1)).deleteByLinkedEntityId(eq(linkedEntityId));
    }

    @Test
    @DisplayName("deleteUserByLinkedEntityId - should return false for null or blank id")
    void testDeleteByLinkedEntityIdInvalid() {
        boolean resultNull = userService.deleteUserByLinkedEntityId(null);
        boolean resultBlank = userService.deleteUserByLinkedEntityId("");

        assertFalse(resultNull);
        assertFalse(resultBlank);
    }

    @Test
    @DisplayName("updateUserByLinkedEntityId - should return true on success")
    void testUpdateUserByLinkedEntityIdSuccess() {
        String linkedEntityId = "entity-789";
        String newUsername = "bob";
        List<String> newRoles = Arrays.asList("STAFF");
        when(mockUserDAO.updateUserByLinkedEntityId(linkedEntityId, newUsername, newRoles)).thenReturn(true);

        boolean result = userService.updateUserByLinkedEntityId(linkedEntityId, newUsername, newRoles);

        assertTrue(result);
        verify(mockUserDAO, times(1)).updateUserByLinkedEntityId(eq(linkedEntityId), eq(newUsername), eq(newRoles));
    }

    @Test
    @DisplayName("updateUserByLinkedEntityId - should return false for invalid linkedEntityId")
    void testUpdateUserByLinkedEntityIdInvalidId() {
        boolean result = userService.updateUserByLinkedEntityId(null, "u", Arrays.asList("A"));
        assertFalse(result);
    }

    @Test
    @DisplayName("updateUserByLinkedEntityId - should return false when DAO throws")
    void testUpdateUserByLinkedEntityIdException() {
        String linkedEntityId = "entity-ex";
        when(mockUserDAO.updateUserByLinkedEntityId(linkedEntityId, "x", Arrays.asList("R")))
            .thenThrow(new RuntimeException("DB error"));

        boolean result = userService.updateUserByLinkedEntityId(linkedEntityId, "x", Arrays.asList("R"));

        assertFalse(result);
    }
}
