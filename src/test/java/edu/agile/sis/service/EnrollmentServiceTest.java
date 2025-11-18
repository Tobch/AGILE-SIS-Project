package edu.agile.sis.service;

import edu.agile.sis.dao.EnrollmentDAO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Tests")
class EnrollmentServiceTest {
    @Mock
    private EnrollmentDAO mockEnrollmentDAO;
    
    private EnrollmentService enrollmentService;
    private EntityService mockEntityService;

    @BeforeEach
    void setUp() {
        mockEntityService = mock(EntityService.class);
        
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("enrollments")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        enrollmentService = new EnrollmentService();
        
        // Inject mocks using reflection
        try {
            java.lang.reflect.Field enrollField = EnrollmentService.class.getDeclaredField("enrollmentDAO");
            enrollField.setAccessible(true);
            enrollField.set(enrollmentService, mockEnrollmentDAO);
            
            java.lang.reflect.Field entityField = EnrollmentService.class.getDeclaredField("entityService");
            entityField.setAccessible(true);
            entityField.set(enrollmentService, mockEntityService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    @Test
    @DisplayName("listByStudent - should return enrollments for student")
    void testListByStudentSuccess() {
        String studentId = "student-001";
        List<Document> enrollments = Arrays.asList(
            new Document("studentId", studentId).append("courseCode", "CS101"),
            new Document("studentId", studentId).append("courseCode", "CS102")
        );
        when(mockEnrollmentDAO.listByStudent(studentId)).thenReturn(enrollments);

        List<Document> result = enrollmentService.listByStudent(studentId);

        assertEquals(2, result.size());
        verify(mockEnrollmentDAO, times(1)).listByStudent(studentId);
    }

    @Test
    @DisplayName("listByCourse - should return enrollments for course")
    void testListByCourseSuccess() {
        String courseCode = "CS101";
        List<Document> enrollments = Arrays.asList(
            new Document("courseCode", courseCode).append("studentId", "student-001"),
            new Document("courseCode", courseCode).append("studentId", "student-002")
        );
        when(mockEnrollmentDAO.listByCourse(courseCode)).thenReturn(enrollments);

        List<Document> result = enrollmentService.listByCourse(courseCode);

        assertEquals(2, result.size());
        verify(mockEnrollmentDAO, times(1)).listByCourse(courseCode);
    }

    @Test
    @DisplayName("isStudentRegisteredForCourse - should return true when registered")
    void testIsStudentRegisteredTrue() {
        String studentId = "student-001";
        String courseCode = "CS101";
        Document enrollment = new Document("studentId", studentId).append("courseCode", courseCode);
        when(mockEnrollmentDAO.find(studentId, courseCode)).thenReturn(enrollment);

        boolean result = enrollmentService.isStudentRegisteredForCourse(studentId, courseCode);

        assertTrue(result);
        verify(mockEnrollmentDAO, times(1)).find(studentId, courseCode);
    }

    @Test
    @DisplayName("isStudentRegisteredForCourse - should return false when not registered")
    void testIsStudentRegisteredFalse() {
        String studentId = "student-001";
        String courseCode = "CS101";
        when(mockEnrollmentDAO.find(studentId, courseCode)).thenReturn(null);

        boolean result = enrollmentService.isStudentRegisteredForCourse(studentId, courseCode);

        assertFalse(result);
        verify(mockEnrollmentDAO, times(1)).find(studentId, courseCode);
    }

    @Test
    @DisplayName("registerStudentToCourse - should register student")
    void testRegisterStudentSuccess() {
        String studentId = "student-001";
        String courseCode = "CS101";
        when(mockEnrollmentDAO.find(studentId, courseCode)).thenReturn(null);
        when(mockEnrollmentDAO.countDistinctCoursesByStudent(studentId)).thenReturn(2);
        
        Document student = new Document("_id", studentId)
            .append("attributes", Arrays.asList(
                new Document("key", "gpa").append("value", 3.5)
            ));
        when(mockEntityService.getEntityById(studentId)).thenReturn(student);

        boolean result = enrollmentService.registerStudentToCourse(studentId, courseCode);

        assertTrue(result);
        verify(mockEnrollmentDAO, times(1)).insertEnrollment(any());
    }

    @Test
    @DisplayName("unregisterStudentFromCourse - should unregister student")
    void testUnregisterStudentSuccess() {
        String studentId = "student-001";
        String courseCode = "CS101";
        when(mockEnrollmentDAO.deleteByStudentAndCourse(studentId, courseCode)).thenReturn(true);

        boolean result = enrollmentService.unregisterStudentFromCourse(studentId, courseCode);

        assertTrue(result);
        verify(mockEnrollmentDAO, times(1)).deleteByStudentAndCourse(studentId, courseCode);
    }
}
