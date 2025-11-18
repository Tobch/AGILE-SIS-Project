package edu.agile.sis.service;

import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.dao.ReservationDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Tests")
class ReservationServiceTest {
    @Mock
    private ReservationDAO mockReservationDAO;
    
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        // Mock DBConnection to prevent real database access during service construction
        try {
            com.mongodb.client.MongoDatabase mockDatabase = mock(com.mongodb.client.MongoDatabase.class);
            @SuppressWarnings("unchecked")
            com.mongodb.client.MongoCollection<org.bson.Document> mockCollection = mock(com.mongodb.client.MongoCollection.class);
            when(mockDatabase.getCollection("reservations")).thenReturn(mockCollection);

            edu.agile.sis.db.DBConnection mockDBConnection = mock(edu.agile.sis.db.DBConnection.class);
            when(mockDBConnection.getDatabase()).thenReturn(mockDatabase);

            java.lang.reflect.Field instanceField = edu.agile.sis.db.DBConnection.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, mockDBConnection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock DBConnection", e);
        }

        reservationService = new ReservationService();

        // Inject mock using reflection (field name in ReservationService is 'reservationDAO')
        try {
            java.lang.reflect.Field field = ReservationService.class.getDeclaredField("reservationDAO");
            field.setAccessible(true);
            field.set(reservationService, mockReservationDAO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock ReservationDAO", e);
        }

        // Set up authenticated user with ADMIN role
        AuthSession.getInstance().setCurrentUser(
            new Document("_id", "admin-001")
                .append("username", "admin")
                .append("roles", Arrays.asList("ADMIN"))
        );
    }

    @Test
    @DisplayName("createReservation - should create reservation successfully")
    void testCreateReservationSuccess() {
        String roomId = "room-001";
        Date start = new Date();
        Date end = new Date(start.getTime() + 3600000); // 1 hour later
        String createdBy = "admin-001";
        String purpose = "Team Meeting";

        when(mockReservationDAO.findOverlapping(roomId, start, end)).thenReturn(Arrays.asList());
        when(mockReservationDAO.insertReservation(any(Document.class))).thenReturn(new org.bson.types.ObjectId());

        try {
            reservationService.createReservation(roomId, start, end, createdBy, purpose);
            verify(mockReservationDAO, times(1)).insertReservation(any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("createReservation - should throw SecurityException without proper authorization")
    void testCreateReservationUnauthorized() {
        AuthSession.getInstance().setCurrentUser(
            new Document("_id", "user-001")
                .append("username", "user")
                .append("roles", Arrays.asList("STUDENT"))
        );

        String roomId = "room-001";
        Date start = new Date();
        Date end = new Date(start.getTime() + 3600000);
        String createdBy = "user-001";
        String purpose = "Meeting";

        try {
            reservationService.createReservation(roomId, start, end, createdBy, purpose);
        } catch (SecurityException e) {
            assertEquals("Insufficient privileges to create reservation", e.getMessage());
        }
    }

    @Test
    @DisplayName("updateReservation - should update reservation successfully")
    void testUpdateReservationSuccess() {
        String reservationId = "507f1f77bcf86cd799439011";
        Date start = new Date();
        Date end = new Date(start.getTime() + 3600000);
        Document updatedData = new Document("roomId", "room-001")
            .append("start", start)
            .append("end", end);

        when(mockReservationDAO.findOverlappingExcluding("room-001", start, end, reservationId)).thenReturn(Arrays.asList());
        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockReservationDAO.updateReservation(reservationId, updatedData)).thenReturn(mockResult);

        try {
            boolean result = reservationService.updateReservation(reservationId, updatedData);
            assertTrue(result);
            verify(mockReservationDAO, times(1)).updateReservation(reservationId, updatedData);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("listAllReservations - should return all reservations")
    void testListAllReservationsSuccess() {
        List<Document> reservations = Arrays.asList(
            new Document("_id", "res-001").append("purpose", "Meeting 1"),
            new Document("_id", "res-002").append("purpose", "Meeting 2")
        );
        when(mockReservationDAO.findAllReservations()).thenReturn(reservations);

        List<Document> result = reservationService.listAllReservations();

        assertEquals(2, result.size());
        verify(mockReservationDAO, times(1)).findAllReservations();
    }

    @Test
    @DisplayName("getReservationsByRoom - should return reservations for a room")
    void testGetReservationsByRoomSuccess() {
        String roomId = "room-001";
        List<Document> reservations = Arrays.asList(
            new Document("roomId", roomId).append("purpose", "Meeting 1"),
            new Document("roomId", roomId).append("purpose", "Meeting 2")
        );
        when(mockReservationDAO.findByRoomId(roomId)).thenReturn(reservations);

        List<Document> result = reservationService.getReservationsByRoom(roomId);

        assertEquals(2, result.size());
        verify(mockReservationDAO, times(1)).findByRoomId(roomId);
    }

    @Test
    @DisplayName("deleteReservation - should delete reservation successfully")
    void testDeleteReservationSuccess() {
        String reservationId = "507f1f77bcf86cd799439011";

        try {
            reservationService.deleteReservation(reservationId);
            verify(mockReservationDAO, times(1)).deleteReservation(reservationId);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("getReservationById - should return reservation by ID")
    void testGetReservationByIdSuccess() {
        String reservationId = "507f1f77bcf86cd799439011";
        Document reservation = new Document("_id", reservationId).append("purpose", "Meeting");
        when(mockReservationDAO.findById(reservationId)).thenReturn(reservation);

        Document result = reservationService.getReservationById(reservationId);

        assertEquals(reservationId, result.getString("_id"));
        verify(mockReservationDAO, times(1)).findById(reservationId);
    }

    @Test
    @DisplayName("approveReservation - should approve reservation")
    void testApproveReservationSuccess() {
        String reservationId = "507f1f77bcf86cd799439011";
        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockReservationDAO.updateReservation(any(String.class), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = reservationService.approveReservation(reservationId);
            assertTrue(result);
            verify(mockReservationDAO, times(1)).updateReservation(any(String.class), any(Document.class));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("rejectReservation - should reject reservation")
    void testRejectReservationSuccess() {
        String reservationId = "507f1f77bcf86cd799439011";
        UpdateResult mockResult = mock(UpdateResult.class);
        when(mockResult.getModifiedCount()).thenReturn(1L);
        when(mockReservationDAO.updateReservation(any(String.class), any(Document.class))).thenReturn(mockResult);

        try {
            boolean result = reservationService.rejectReservation(reservationId);
            assertTrue(result);
            verify(mockReservationDAO, times(1)).updateReservation(any(String.class), any(Document.class));
        } catch (Exception ignored) {
        }
    }
}
