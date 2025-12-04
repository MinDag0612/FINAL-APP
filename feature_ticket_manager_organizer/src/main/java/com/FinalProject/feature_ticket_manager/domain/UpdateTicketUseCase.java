package com.FinalProject.feature_ticket_manager.domain;

import androidx.annotation.NonNull;

import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_ticket_manager.data.TicketManagerRepository;
import com.google.android.gms.tasks.Task;

/**
 * Use Case để cập nhật thông tin vé
 */
public class UpdateTicketUseCase {
    
    private final TicketManagerRepository repository;

    public UpdateTicketUseCase(TicketManagerRepository repository) {
        this.repository = repository;
    }

    public Task<Void> execute(@NonNull String eventId, 
                             @NonNull String ticketId, 
                             @NonNull TicketInfor ticket) {
        return repository.updateTicket(eventId, ticketId, ticket);
    }

    public Task<Void> updateQuantity(@NonNull String eventId, 
                                     @NonNull String ticketId, 
                                     int newQuantity) {
        return repository.updateTicketQuantity(eventId, ticketId, newQuantity);
    }

    public Task<Void> updatePrice(@NonNull String eventId, 
                                  @NonNull String ticketId, 
                                  long newPrice) {
        return repository.updateTicketPrice(eventId, ticketId, newPrice);
    }
}
