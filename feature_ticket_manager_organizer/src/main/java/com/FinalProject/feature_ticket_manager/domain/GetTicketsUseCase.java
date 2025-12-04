package com.FinalProject.feature_ticket_manager.domain;

import androidx.annotation.NonNull;

import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_ticket_manager.data.TicketManagerRepository;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * Use Case để lấy danh sách vé của sự kiện
 */
public class GetTicketsUseCase {
    
    private final TicketManagerRepository repository;

    public GetTicketsUseCase(TicketManagerRepository repository) {
        this.repository = repository;
    }

    public Task<List<TicketInfor>> execute(@NonNull String eventId) {
        return repository.getTicketsForEvent(eventId);
    }
}
