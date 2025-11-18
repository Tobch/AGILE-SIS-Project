package edu.agile.sis.service;

import edu.agile.sis.dao.MessageDAO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Tests")
class MessageServiceTest {
    @Mock
    private MessageDAO mockMessageDAO;
    
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<org.bson.Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("messages")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        messageService = new MessageService();

        // Inject mock using reflection (field name in MessageService is 'dao')
        try {
            java.lang.reflect.Field field = MessageService.class.getDeclaredField("dao");
            field.setAccessible(true);
            field.set(messageService, mockMessageDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock MessageDAO", e);
        }
    }

    @Test
    @DisplayName("sendMessage - should send message successfully")
    void testSendMessageSuccess() {
        String studentId = "student-001";
        String staffId = "staff-001";
        String senderId = "student-001";
        String body = "Hello, I need help with assignment 1";

        messageService.sendMessage(studentId, staffId, senderId, body);

        verify(mockMessageDAO, times(1)).insertMessage(any(Document.class));
    }

    @Test
    @DisplayName("getThread - should return conversation thread")
    void testGetThreadSuccess() {
        String studentId = "student-001";
        String staffId = "staff-001";
        List<Document> thread = Arrays.asList(
            new Document("studentId", studentId).append("staffId", staffId).append("body", "Hello"),
            new Document("studentId", studentId).append("staffId", staffId).append("body", "Hi there")
        );
        when(mockMessageDAO.findThreadBetween(studentId, staffId)).thenReturn(thread);

        List<Document> result = messageService.getThread(studentId, staffId);

        assertEquals(2, result.size());
        verify(mockMessageDAO, times(1)).findThreadBetween(studentId, staffId);
    }

    @Test
    @DisplayName("getByStudent - should return all messages for student")
    void testGetByStudentSuccess() {
        String studentId = "student-001";
        List<Document> messages = Arrays.asList(
            new Document("studentId", studentId).append("body", "Message 1"),
            new Document("studentId", studentId).append("body", "Message 2")
        );
        when(mockMessageDAO.findByStudent(studentId)).thenReturn(messages);

        List<Document> result = messageService.getByStudent(studentId);

        assertEquals(2, result.size());
        verify(mockMessageDAO, times(1)).findByStudent(studentId);
    }

    @Test
    @DisplayName("getByStaff - should return all messages for staff")
    void testGetByStaffSuccess() {
        String staffId = "staff-001";
        List<Document> messages = Arrays.asList(
            new Document("staffId", staffId).append("body", "Reply 1"),
            new Document("staffId", staffId).append("body", "Reply 2")
        );
        when(mockMessageDAO.findByStaff(staffId)).thenReturn(messages);

        List<Document> result = messageService.getByStaff(staffId);

        assertEquals(2, result.size());
        verify(mockMessageDAO, times(1)).findByStaff(staffId);
    }

    @Test
    @DisplayName("markRead - should mark message as read")
    void testMarkReadSuccess() {
        String messageId = "msg-001";

        messageService.markRead(messageId);

        verify(mockMessageDAO, times(1)).markRead(messageId);
    }

    @Test
    @DisplayName("delete - should delete message")
    void testDeleteSuccess() {
        String messageId = "msg-001";

        messageService.delete(messageId);

        verify(mockMessageDAO, times(1)).delete(messageId);
    }
}
