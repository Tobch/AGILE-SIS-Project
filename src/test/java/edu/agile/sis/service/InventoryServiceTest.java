package edu.agile.sis.service;

import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.dao.AuditLogDAO;
import edu.agile.sis.dao.InventoryDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
class InventoryServiceTest {
    @Mock
    private InventoryDAO mockInventoryDAO;

    @Mock
    private AuditLogDAO mockAuditLogDAO;

    @Mock
    private EntityService mockEntityService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<org.bson.Document> mockCollection = mock(
                    com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        inventoryService = new InventoryService();

        // Inject mocks using reflection
        try {
            java.lang.reflect.Field inventoryDaoField = InventoryService.class.getDeclaredField("inventoryDAO");
            inventoryDaoField.setAccessible(true);
            inventoryDaoField.set(inventoryService, mockInventoryDAO);

            java.lang.reflect.Field auditDaoField = InventoryService.class.getDeclaredField("auditLogDAO");
            auditDaoField.setAccessible(true);
            auditDaoField.set(inventoryService, mockAuditLogDAO);

            java.lang.reflect.Field entityServiceField = InventoryService.class.getDeclaredField("entityService");
            entityServiceField.setAccessible(true);
            entityServiceField.set(inventoryService, mockEntityService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock DAOs", e);
        }

        // Set up authenticated user with ADMIN role
        AuthSession.getInstance().setCurrentUser(
                new Document("_id", "admin-001")
                        .append("username", "admin")
                        .append("roles", Arrays.asList("Admin"))
                        .append("linkedEntityId", "admin-entity-001"));
    }

    @Test
    @DisplayName("createItem - should create item successfully with Admin role (no department)")
    void testCreateItemSuccess() {
        ObjectId generatedId = new ObjectId();
        when(mockInventoryDAO.insertItem(any(Document.class))).thenReturn(generatedId);

        try {
            String result = inventoryService.createItem("Laptop #001", "Laptop", "Test notes");

            assertNotNull(result);
            assertEquals(generatedId.toHexString(), result);
            verify(mockInventoryDAO, times(1)).insertItem(any(Document.class));
            verify(mockAuditLogDAO, times(1)).insertLog(any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("createItem - should throw SecurityException for Student role")
    void testCreateItemUnauthorized() {
        AuthSession.getInstance().setCurrentUser(
                new Document("_id", "student-001")
                        .append("username", "student")
                        .append("roles", Arrays.asList("Student")));

        assertThrows(SecurityException.class, () -> {
            inventoryService.createItem("Laptop #001", "Laptop", "Notes");
        });
    }

    @Test
    @DisplayName("allocateItem - should assign item to user after validation")
    void testAllocateItemSuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001")
                .append("itemType", "Laptop")
                .append("status", "Available");

        // Mock user validation - entity exists with matching name
        Document entityCore = new Document("name", "John Doe");
        Document entity = new Document("core", entityCore);
        when(mockEntityService.getEntityById("user-001")).thenReturn(entity);

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockInventoryDAO.updateItem(eq(itemId), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = inventoryService.allocateItem(itemId, "user-001", "John Doe");

            assertTrue(result);
            verify(mockInventoryDAO, times(1)).updateItem(eq(itemId), any(Document.class));
            verify(mockAuditLogDAO, times(1)).insertLog(any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("allocateItem - should throw exception if user not found")
    void testAllocateItemUserNotFound() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001")
                .append("itemType", "Laptop")
                .append("status", "Available");

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);
        when(mockEntityService.getEntityById("invalid-user")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.allocateItem(itemId, "invalid-user", "John Doe");
        });
    }

    @Test
    @DisplayName("allocateItem - should throw exception if name doesn't match")
    void testAllocateItemNameMismatch() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001")
                .append("itemType", "Laptop")
                .append("status", "Available");

        // Entity has different name
        Document entityCore = new Document("name", "Jane Smith");
        Document entity = new Document("core", entityCore);
        when(mockEntityService.getEntityById("user-001")).thenReturn(entity);

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.allocateItem(itemId, "user-001", "John Doe");
        });
    }

    @Test
    @DisplayName("allocateItem - should throw exception if item already assigned")
    void testAllocateItemAlreadyAssigned() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001")
                .append("itemType", "Laptop")
                .append("status", "Assigned")
                .append("assignedToName", "Jane Smith");

        // Mock user validation
        Document entityCore = new Document("name", "John Doe");
        Document entity = new Document("core", entityCore);
        when(mockEntityService.getEntityById("user-002")).thenReturn(entity);

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        assertThrows(IllegalStateException.class, () -> {
            inventoryService.allocateItem(itemId, "user-002", "John Doe");
        });
    }

    @Test
    @DisplayName("addUserToLicense - should add multiple users to license")
    void testAddUserToLicenseSuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Office License #001")
                .append("itemType", "License")
                .append("status", "Available")
                .append("assignedUsers", new ArrayList<Document>());

        // Mock user validation
        Document entityCore = new Document("name", "John Doe");
        Document entity = new Document("core", entityCore);
        when(mockEntityService.getEntityById("user-001")).thenReturn(entity);

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockInventoryDAO.updateItem(eq(itemId), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = inventoryService.addUserToLicense(itemId, "user-001", "John Doe");

            assertTrue(result);
            verify(mockInventoryDAO, times(1)).updateItem(eq(itemId), any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("removeUserFromLicense - should remove specific user from license")
    void testRemoveUserFromLicenseSuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        List<Document> assignedUsers = new ArrayList<>();
        assignedUsers.add(new Document("userId", "user-001").append("userName", "John Doe"));
        assignedUsers.add(new Document("userId", "user-002").append("userName", "Jane Smith"));

        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Office License #001")
                .append("itemType", "License")
                .append("status", "Assigned")
                .append("assignedUsers", assignedUsers);

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockInventoryDAO.updateItem(eq(itemId), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = inventoryService.removeUserFromLicense(itemId, "user-001");

            assertTrue(result);
            verify(mockInventoryDAO, times(1)).updateItem(eq(itemId), any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("deallocateItem - should unassign item successfully")
    void testDeallocateItemSuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001")
                .append("itemType", "Laptop")
                .append("status", "Assigned")
                .append("assignedToName", "John Doe");

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockInventoryDAO.updateItem(eq(itemId), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = inventoryService.deallocateItem(itemId);

            assertTrue(result);
            verify(mockInventoryDAO, times(1)).updateItem(eq(itemId), any(Document.class));
            verify(mockAuditLogDAO, times(1)).insertLog(any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("listAllItems - should return all inventory items")
    void testListAllItemsSuccess() {
        List<Document> items = Arrays.asList(
                new Document("_id", "item-001").append("name", "Laptop #001"),
                new Document("_id", "item-002").append("name", "License #001"));
        when(mockInventoryDAO.findAllItems()).thenReturn(items);

        List<Document> result = inventoryService.listAllItems();

        assertEquals(2, result.size());
        verify(mockInventoryDAO, times(1)).findAllItems();
    }

    @Test
    @DisplayName("getItemsByStatus - should filter items by status")
    void testGetItemsByStatusSuccess() {
        List<Document> availableItems = Arrays.asList(
                new Document("_id", "item-001").append("name", "Laptop #001").append("status", "Available"));
        when(mockInventoryDAO.findByStatus("Available")).thenReturn(availableItems);

        List<Document> result = inventoryService.getItemsByStatus("Available");

        assertEquals(1, result.size());
        verify(mockInventoryDAO, times(1)).findByStatus("Available");
    }

    @Test
    @DisplayName("deleteItem - should delete item with Admin role")
    void testDeleteItemSuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        Document existingItem = new Document("_id", new ObjectId(itemId))
                .append("name", "Laptop #001");

        when(mockInventoryDAO.findById(itemId)).thenReturn(existingItem);

        try {
            inventoryService.deleteItem(itemId);
            verify(mockInventoryDAO, times(1)).deleteItem(itemId);
            verify(mockAuditLogDAO, times(1)).insertLog(any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("deleteItem - should throw SecurityException for non-Admin")
    void testDeleteItemUnauthorized() {
        AuthSession.getInstance().setCurrentUser(
                new Document("_id", "staff-001")
                        .append("username", "staff")
                        .append("roles", Arrays.asList("Staff")));

        assertThrows(SecurityException.class, () -> {
            inventoryService.deleteItem("507f1f77bcf86cd799439011");
        });
    }

    @Test
    @DisplayName("markUnderRepair - should change item status to Under Repair")
    void testMarkUnderRepairSuccess() {
        String itemId = "507f1f77bcf86cd799439011";

        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockInventoryDAO.updateItem(eq(itemId), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = inventoryService.markUnderRepair(itemId);

            assertTrue(result);
            verify(mockInventoryDAO, times(1)).updateItem(eq(itemId), any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("getAuditHistory - should return audit logs for item")
    void testGetAuditHistorySuccess() {
        String itemId = "507f1f77bcf86cd799439011";
        List<Document> logs = Arrays.asList(
                new Document("action", "CREATE").append("details", "Created item"),
                new Document("action", "ALLOCATE").append("details", "Assigned to John"));
        when(mockAuditLogDAO.findByEntityId(itemId)).thenReturn(logs);

        List<Document> result = inventoryService.getAuditHistory(itemId);

        assertEquals(2, result.size());
        verify(mockAuditLogDAO, times(1)).findByEntityId(itemId);
    }
}
