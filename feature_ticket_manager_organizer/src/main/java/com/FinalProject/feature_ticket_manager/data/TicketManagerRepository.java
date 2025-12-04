package com.FinalProject.feature_ticket_manager.data;

import androidx.annotation.NonNull;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.core.util.TicketS_Infor_API;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TicketManagerRepository - Quản lý CRUD cho Tickets_infor subcollection
 * 
 * Luồng: Presentation -> Repository -> Core API (TicketS_Infor_API, Event_API)
 */
public class TicketManagerRepository {

    private static volatile TicketManagerRepository INSTANCE;
    private final FirebaseFirestore db;

    private TicketManagerRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static TicketManagerRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (TicketManagerRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TicketManagerRepository();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Lấy tất cả loại vé của 1 sự kiện
     */
    public Task<List<TicketInfor>> getTicketsForEvent(@NonNull String eventId) {
        return TicketS_Infor_API.getTicketInforByEventId(eventId)
                .continueWith(task -> {
                    List<TicketInfor> result = new ArrayList<>();
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return result;
                    }

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        TicketInfor ticket = doc.toObject(TicketInfor.class);
                        if (ticket != null) {
                            result.add(ticket);
                        }
                    }
                    return result;
                });
    }

    /**
     * Thêm loại vé mới vào sự kiện
     */
    public Task<Void> addTicket(@NonNull String eventId, @NonNull TicketInfor ticket) {
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .add(ticket.toMap())
                .continueWith(task -> null);
    }

    /**
     * Cập nhật thông tin loại vé (giá, số lượng)
     */
    public Task<Void> updateTicket(@NonNull String eventId, 
                                   @NonNull String ticketId, 
                                   @NonNull TicketInfor ticket) {
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .document(ticketId)
                .set(ticket.toMap(), SetOptions.merge());
    }

    /**
     * Cập nhật chỉ số lượng vé
     */
    public Task<Void> updateTicketQuantity(@NonNull String eventId, 
                                           @NonNull String ticketId, 
                                           int newQuantity) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(StoreField.TicketFields.TICKETS_QUANTITY, newQuantity);
        
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .document(ticketId)
                .update(updates);
    }

    /**
     * Cập nhật giá vé
     */
    public Task<Void> updateTicketPrice(@NonNull String eventId, 
                                        @NonNull String ticketId, 
                                        long newPrice) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(StoreField.TicketFields.TICKETS_PRICE, newPrice);
        
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .document(ticketId)
                .update(updates);
    }

    /**
     * Xóa loại vé
     */
    public Task<Void> deleteTicket(@NonNull String eventId, @NonNull String ticketId) {
        return db.collection(StoreField.EVENTS)
                .document(eventId)
                .collection(StoreField.TICKETS_INFOR)
                .document(ticketId)
                .delete();
    }

    /**
     * Lấy thông tin 1 sự kiện
     */
    public Task<DocumentSnapshot> getEvent(@NonNull String eventId) {
        return Event_API.getEventById(eventId);
    }

    /**
     * Lấy danh sách sự kiện của organizer
     */
    public Task<QuerySnapshot> getOrganizerEvents(@NonNull String organizerUid) {
        return Event_API.getEventsByOrganizer(organizerUid, 100);
    }
}
