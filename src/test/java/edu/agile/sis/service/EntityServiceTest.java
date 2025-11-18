package edu.agile.sis.service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EntityService Tests")
class EntityServiceTest {
    
    private EntityService entityService;
    @SuppressWarnings("unchecked")
    private com.mongodb.client.MongoCollection<Document> mockCollection;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            mockCollection = mock(com.mongodb.client.MongoCollection.class);
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            when(mockDatabase.getCollection("entities")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        entityService = new EntityService("entities");

        // Inject mock collection using reflection
        try {
            java.lang.reflect.Field field = EntityService.class.getDeclaredField("coll");
            field.setAccessible(true);
            field.set(entityService, mockCollection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock collection", e);
        }
    }

    @Test
    @DisplayName("getEntityById - should return entity by entityId")
    void testGetEntityByIdSuccess() {
        String entityId = "student-001";
        Document entity = new Document("_id", new org.bson.types.ObjectId())
            .append("entityId", entityId)
            .append("name", "John Doe");

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(entity);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);

        Document result = entityService.getEntityById(entityId);

        assertNotNull(result);
        assertEquals(entityId, result.getString("entityId"));
    }

    @Test
    @DisplayName("getEntityById - should return null when entityId is null")
    void testGetEntityByIdNull() {
        Document result = entityService.getEntityById(null);

        assertNull(result);
    }

    @Test
    @DisplayName("getEntitiesByType - should return entities by type")
    void testGetEntitiesByTypeSuccess() {
        String typeName = "student";
        ArrayList<Document> entities = new ArrayList<>(Arrays.asList(
            new Document("type", typeName).append("name", "John Doe"),
            new Document("type", typeName).append("name", "Jane Smith")
        ));

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        // Use any() to match any ArrayList argument and return our prepared entities
        when(mockIterable.into(any())).thenReturn(entities);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);

        List<Document> result = entityService.getEntitiesByType(typeName);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("createEntity - should create entity successfully")
    void testCreateEntitySuccess() {
        Document entity = new Document("_id", new org.bson.types.ObjectId())
            .append("name", "New Entity")
            .append("type", "student");

        boolean result = entityService.createEntity(entity);

        assertTrue(result);
    }

    @Test
    @DisplayName("createEntity - should return false when document is null")
    void testCreateEntityNull() {
        boolean result = entityService.createEntity(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteEntity - should delete entity successfully")
    void testDeleteEntitySuccess() {
        String entityId = "student-001";
        org.bson.types.ObjectId objectId = new org.bson.types.ObjectId();
        Document entity = new Document("_id", objectId)
            .append("entityId", entityId)
            .append("name", "John Doe");

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(entity);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);

        boolean result = entityService.deleteEntity(entityId);

        assertTrue(result);
    }

    @Test
    @DisplayName("deleteEntity - should return false when entity not found")
    void testDeleteEntityNotFound() {
        String entityId = "nonexistent-001";

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(null);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);

        boolean result = entityService.deleteEntity(entityId);

        assertFalse(result);
    }

    @Test
    @DisplayName("updateEntityMerge - should merge core updates successfully")
    void testUpdateEntityMergeSuccess() {
        String entityId = "student-001";
        org.bson.types.ObjectId objectId = new org.bson.types.ObjectId();
        Document core = new Document("gpa", 3.5).append("enrolledSince", "2024-01-01");
        Document entity = new Document("_id", objectId)
            .append("entityId", entityId)
            .append("core", core);

        Map<String, Object> coreUpdates = new HashMap<>();
        coreUpdates.put("gpa", 3.8);

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(entity);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);
        
        UpdateResult mockUpdateResult = mock(UpdateResult.class);
        when(mockUpdateResult.getModifiedCount()).thenReturn(1L);
        when(mockCollection.updateOne(any(org.bson.conversions.Bson.class), 
                                      any(org.bson.conversions.Bson.class))).thenReturn(mockUpdateResult);

        boolean result = entityService.updateEntityMerge(entityId, coreUpdates, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("updateEntityMerge - should return false when entity not found")
    void testUpdateEntityMergeNotFound() {
        String entityId = "nonexistent-001";
        Map<String, Object> coreUpdates = new HashMap<>();
        coreUpdates.put("gpa", 3.8);

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(null);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);

        boolean result = entityService.updateEntityMerge(entityId, coreUpdates, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("replaceAttributes - should replace attributes successfully")
    void testReplaceAttributesSuccess() {
        String entityId = "student-001";
        org.bson.types.ObjectId objectId = new org.bson.types.ObjectId();
        Document entity = new Document("_id", objectId)
            .append("entityId", entityId)
            .append("core", new Document());

        List<Document> newAttributes = Arrays.asList(
            new Document("key", "attr1").append("value", "value1"),
            new Document("key", "attr2").append("value", "value2")
        );

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(entity);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);
        
        UpdateResult mockUpdateResult = mock(UpdateResult.class);
        when(mockUpdateResult.getModifiedCount()).thenReturn(1L);
        when(mockCollection.updateOne(any(org.bson.conversions.Bson.class), 
                                      any(org.bson.conversions.Bson.class))).thenReturn(mockUpdateResult);

        boolean result = entityService.replaceAttributes(entityId, newAttributes);

        assertTrue(result);
    }

    @Test
    @DisplayName("updateCoreField - should update single core field successfully")
    void testUpdateCoreFieldSuccess() {
        String entityId = "student-001";
        org.bson.types.ObjectId objectId = new org.bson.types.ObjectId();
        Document core = new Document("gpa", 3.5);
        Document entity = new Document("_id", objectId)
            .append("entityId", entityId)
            .append("core", core);

        @SuppressWarnings("unchecked")
        FindIterable<Document> mockIterable = mock(FindIterable.class);
        when(mockIterable.first()).thenReturn(entity);
        when(mockCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(mockIterable);
        
        UpdateResult mockUpdateResult = mock(UpdateResult.class);
        when(mockUpdateResult.getModifiedCount()).thenReturn(1L);
        when(mockCollection.updateOne(any(org.bson.conversions.Bson.class), 
                                      any(org.bson.conversions.Bson.class))).thenReturn(mockUpdateResult);

        boolean result = entityService.updateCoreField(entityId, "gpa", 3.9);

        assertTrue(result);
    }
}
