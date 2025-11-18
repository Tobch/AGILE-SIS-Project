package edu.agile.sis.service;

import edu.agile.sis.dao.AttributeMetaDAO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttributeService Tests")
class AttributeServiceTest {
    @Mock
    private AttributeMetaDAO mockAttributeMetaDAO;
    
    private AttributeService attributeService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<org.bson.Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("attribute_meta")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        attributeService = new AttributeService();

        // Inject mock using reflection (field name in AttributeService is 'dao')
        try {
            java.lang.reflect.Field field = AttributeService.class.getDeclaredField("dao");
            field.setAccessible(true);
            field.set(attributeService, mockAttributeMetaDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock AttributeMetaDAO", e);
        }
    }

    @Test
    @DisplayName("createAttributeMeta - should create attribute metadata successfully")
    void testCreateAttributeMetaSuccess() {
        Document meta = new Document("key", "gpa_threshold")
            .append("value", "3.5")
            .append("type", "double");

        attributeService.createAttributeMeta(meta);

        verify(mockAttributeMetaDAO, times(1)).insert(eq(meta));
    }

    @Test
    @DisplayName("listAll - should return all attribute metadata")
    void testListAllSuccess() {
        List<Document> metaList = Arrays.asList(
            new Document("key", "gpa_threshold").append("value", "3.5"),
            new Document("key", "max_credits").append("value", "18")
        );
        when(mockAttributeMetaDAO.findAll()).thenReturn(metaList);

        List<Document> result = attributeService.listAll();

        assertEquals(2, result.size());
        verify(mockAttributeMetaDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("listAll - should return empty list when no metadata exist")
    void testListAllEmpty() {
        when(mockAttributeMetaDAO.findAll()).thenReturn(new ArrayList<>());

        List<Document> result = attributeService.listAll();

        assertEquals(0, result.size());
        verify(mockAttributeMetaDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("update - should update attribute metadata successfully")
    void testUpdateSuccess() {
        String key = "gpa_threshold";
        Document updatedData = new Document("value", "3.8");

        attributeService.update(key, updatedData);

        verify(mockAttributeMetaDAO, times(1)).update(eq(key), eq(updatedData));
    }

    @Test
    @DisplayName("delete - should delete attribute metadata by key")
    void testDeleteSuccess() {
        String key = "gpa_threshold";

        attributeService.delete(key);

        verify(mockAttributeMetaDAO, times(1)).delete(eq(key));
    }

    @Test
    @DisplayName("findByKey - should return attribute metadata by key")
    void testFindByKeySuccess() {
        String key = "gpa_threshold";
        Document meta = new Document("key", key).append("value", "3.5");
        when(mockAttributeMetaDAO.findByKey(eq(key))).thenReturn(meta);

        Document result = attributeService.findByKey(key);

        assertNotNull(result);
        assertEquals(key, result.getString("key"));
        verify(mockAttributeMetaDAO, times(1)).findByKey(eq(key));
    }

    @Test
    @DisplayName("findByKey - should return null when attribute not found")
    void testFindByKeyNotFound() {
        String key = "nonexistent_key";
        when(mockAttributeMetaDAO.findByKey(eq(key))).thenReturn(null);

        Document result = attributeService.findByKey(key);

        assertNull(result);
        verify(mockAttributeMetaDAO, times(1)).findByKey(eq(key));
    }
}
