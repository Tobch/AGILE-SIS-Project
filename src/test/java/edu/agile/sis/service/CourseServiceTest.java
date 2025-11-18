package edu.agile.sis.service;

import edu.agile.sis.dao.CourseDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CourseService Tests")
class CourseServiceTest {
    @Mock
    private CourseDAO mockCourseDAO;
    
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("courses")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        courseService = new CourseService();
        
        // Inject mock DAO using reflection
        try {
            java.lang.reflect.Field daoField = CourseService.class.getDeclaredField("dao");
            daoField.setAccessible(true);
            daoField.set(courseService, mockCourseDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock CourseDAO", e);
        }
    }

    private void setupAuthMock(String role) {
        org.bson.Document user = new org.bson.Document("username", "test-user")
            .append("roles", java.util.Arrays.asList(role));
        AuthSession.getInstance().setCurrentUser(user);
    }

    @Test
    @DisplayName("createCourse - should create course with Admin role")
    void testCreateCourseAsAdmin() {
        setupAuthMock("Admin");
        
        Document course = new Document("code", "CS101").append("title", "Intro to CS");
        courseService.createCourse(course);

        verify(mockCourseDAO, times(1)).insertCourse(course);
    }

    @Test
    @DisplayName("listAll - should return all courses")
    void testListAllSuccess() {
        List<Document> courses = Arrays.asList(
            new Document("code", "CS101").append("title", "Intro to CS"),
            new Document("code", "CS102").append("title", "Data Structures")
        );
        when(mockCourseDAO.findAll()).thenReturn(courses);

        List<Document> result = courseService.listAll();

        assertEquals(2, result.size());
        verify(mockCourseDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("findByCode - should return course when found")
    void testFindByCodeSuccess() {
        String code = "CS101";
        Document expected = new Document("code", code).append("title", "Intro to CS");
        when(mockCourseDAO.findByCode(code)).thenReturn(expected);

        Document result = courseService.findByCode(code);

        assertNotNull(result);
        assertEquals(expected, result);
        verify(mockCourseDAO, times(1)).findByCode(code);
    }

    @Test
    @DisplayName("findById - should return course by ID")
    void testFindByIdSuccess() {
        String id = "course-001";
        Document expected = new Document("_id", id).append("code", "CS101");
        when(mockCourseDAO.findById(id)).thenReturn(expected);

        Document result = courseService.findById(id);

        assertNotNull(result);
        assertEquals(expected, result);
        verify(mockCourseDAO, times(1)).findById(id);
    }

    @Test
    @DisplayName("update - should update course with Admin role")
    void testUpdateCourseAsAdmin() {
        setupAuthMock("Admin");
        String id = "course-001";
        Document update = new Document("title", "Updated Title");

        courseService.update(id, update);

        verify(mockCourseDAO, times(1)).update(id, update);
    }

    @Test
    @DisplayName("delete - should delete course with Admin role")
    void testDeleteCourseAsAdmin() {
        setupAuthMock("Admin");
        String id = "course-001";

        courseService.delete(id);

        verify(mockCourseDAO, times(1)).delete(id);
    }

    @Test
    @DisplayName("assignStaffToCourse - should assign staff with Admin role")
    void testAssignStaffToCourseSuccess() {
        setupAuthMock("Admin");
        String courseCode = "CS101";
        String staffId = "prof-001";
        Document course = new Document("_id", new ObjectId())
            .append("code", courseCode)
            .append("assignedStaff", new ArrayList<String>());
        
        when(mockCourseDAO.findByCode(courseCode)).thenReturn(course);

        boolean result = courseService.assignStaffToCourse(courseCode, staffId);

        assertTrue(result);
        verify(mockCourseDAO, times(1)).findByCode(courseCode);
    }
}
