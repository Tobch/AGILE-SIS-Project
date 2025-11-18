package edu.agile.sis.service;

import edu.agile.sis.dao.StaffDAO;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffService Tests")
class StaffServiceTest {
    @Mock
    private StaffDAO mockStaffDAO;
    
    private StaffService staffService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<org.bson.Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("staff")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        staffService = new StaffService();

        // Inject mock using reflection
        try {
            java.lang.reflect.Field field = StaffService.class.getDeclaredField("dao");
            field.setAccessible(true);
            field.set(staffService, mockStaffDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock StaffDAO", e);
        }
    }

    @Test
    @DisplayName("createStaff - should create staff successfully")
    void testCreateStaffSuccess() {
        Document staff = new Document("staffId", "S001")
            .append("name", "John Doe")
            .append("department", "Computer Science");

        staffService.createStaff(staff);

        verify(mockStaffDAO, times(1)).insertStaff(eq(staff));
    }

    @Test
    @DisplayName("listAll - should return all staff")
    void testListAllSuccess() {
        List<Document> staffList = Arrays.asList(
            new Document("_id", "1").append("staffId", "S001").append("name", "John Doe"),
            new Document("_id", "2").append("staffId", "S002").append("name", "Jane Smith")
        );
        when(mockStaffDAO.findAll()).thenReturn(staffList);

        List<Document> result = staffService.listAll();

        assertEquals(2, result.size());
        verify(mockStaffDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("listAll - should return empty list when no staff exist")
    void testListAllEmpty() {
        when(mockStaffDAO.findAll()).thenReturn(new ArrayList<>());

        List<Document> result = staffService.listAll();

        assertEquals(0, result.size());
        verify(mockStaffDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("getById - should return staff by ID")
    void testGetByIdSuccess() {
        String staffId = "507f1f77bcf86cd799439011";
        Document staff = new Document("_id", staffId).append("name", "John Doe");
        when(mockStaffDAO.findById(eq(staffId))).thenReturn(staff);

        Document result = staffService.getById(staffId);

        assertNotNull(result);
        assertEquals(staffId, result.getString("_id"));
        verify(mockStaffDAO, times(1)).findById(eq(staffId));
    }

    @Test
    @DisplayName("getById - should return null when staff not found")
    void testGetByIdNotFound() {
        String staffId = "507f1f77bcf86cd799439011";
        when(mockStaffDAO.findById(eq(staffId))).thenReturn(null);

        Document result = staffService.getById(staffId);

        assertNull(result);
        verify(mockStaffDAO, times(1)).findById(eq(staffId));
    }

    @Test
    @DisplayName("getByStaffId - should return staff by staffId")
    void testGetByStaffIdSuccess() {
        String staffId = "S001";
        Document staff = new Document("staffId", staffId).append("name", "John Doe");
        when(mockStaffDAO.findByStaffId(eq(staffId))).thenReturn(staff);

        Document result = staffService.getByStaffId(staffId);

        assertNotNull(result);
        assertEquals(staffId, result.getString("staffId"));
        verify(mockStaffDAO, times(1)).findByStaffId(eq(staffId));
    }

    @Test
    @DisplayName("getByStaffId - should return null when not found")
    void testGetByStaffIdNotFound() {
        String staffId = "S999";
        when(mockStaffDAO.findByStaffId(eq(staffId))).thenReturn(null);

        Document result = staffService.getByStaffId(staffId);

        assertNull(result);
        verify(mockStaffDAO, times(1)).findByStaffId(eq(staffId));
    }

    @Test
    @DisplayName("update - should update staff with partial data")
    void testUpdatePartialDataSuccess() {
        String staffId = "507f1f77bcf86cd799439011";
        Document updatedData = new Document("department", "Mathematics");

        staffService.update(staffId, updatedData);

        verify(mockStaffDAO, times(1)).update(eq(staffId), eq(updatedData));
    }

    @Test
    @DisplayName("update - should update staff with complete data")
    void testUpdateCompleteDataSuccess() {
        String staffId = "507f1f77bcf86cd799439011";
        Document updatedData = new Document("name", "Jane Doe")
            .append("department", "Computer Science")
            .append("email", "jane@example.com");

        staffService.update(staffId, updatedData);

        verify(mockStaffDAO, times(1)).update(eq(staffId), eq(updatedData));
    }

    @Test
    @DisplayName("delete - should delete staff by ID")
    void testDeleteSuccess() {
        String staffId = "507f1f77bcf86cd799439011";

        staffService.delete(staffId);

        verify(mockStaffDAO, times(1)).delete(eq(staffId));
    }

    @Test
    @DisplayName("deleteByStaffId - should delete staff by staffId")
    void testDeleteByStaffIdSuccess() {
        String staffId = "S001";
        when(mockStaffDAO.deleteByStaffId(eq(staffId))).thenReturn(true);

        boolean result = staffService.deleteByStaffId(staffId);

        assertTrue(result);
        verify(mockStaffDAO, times(1)).deleteByStaffId(eq(staffId));
    }

    @Test
    @DisplayName("deleteByStaffId - should return false when staff not found")
    void testDeleteByStaffIdNotFound() {
        String staffId = "S999";
        when(mockStaffDAO.deleteByStaffId(eq(staffId))).thenReturn(false);

        boolean result = staffService.deleteByStaffId(staffId);

        assertFalse(result);
        verify(mockStaffDAO, times(1)).deleteByStaffId(eq(staffId));
    }

    @Test
    @DisplayName("deleteByStaffId - should return false when staffId is null")
    void testDeleteByStaffIdNull() {
        boolean result = staffService.deleteByStaffId(null);

        assertFalse(result);
        verify(mockStaffDAO, times(0)).deleteByStaffId(any());
    }

    @Test
    @DisplayName("deleteByStaffId - should return false when staffId is blank")
    void testDeleteByStaffIdBlank() {
        boolean result = staffService.deleteByStaffId("   ");

        assertFalse(result);
        verify(mockStaffDAO, times(0)).deleteByStaffId(any());
    }
}
