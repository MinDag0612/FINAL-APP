package com.FinalProject.feature_create_event.domain;

import android.util.Log;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_create_event.data.EventEditorRepository;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateEventUseCase {
    private final EventEditorRepository repo = new EventEditorRepository();

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public void execute(String eventId, Events event, TicketInfor ticketInfor, Callback callback) {
        if (event == null || ticketInfor == null || eventId == null || eventId.isEmpty()) {
            if (callback != null) callback.onFailure("Thiếu dữ liệu sự kiện hoặc vé");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        try {
            Date startDate = sdf.parse(event.getEvent_start());
            Date endDate = sdf.parse(event.getEvent_end());
            if (startDate.after(endDate)) {
                if (callback != null) callback.onFailure("Giờ bắt đầu phải trước giờ kết thúc");
                return;
            }
        } catch (Exception e) {
            Log.d("UpdateEventUseCase", "execute: " + e);
            if (callback != null) callback.onFailure("Lỗi định dạng thời gian");
            return;
        }

        if (event.getBase_price() <= 0 || ticketInfor.getTickets_price() <= 0) {
            if (callback != null) callback.onFailure("Giá vé phải lớn hơn 0");
            return;
        }

        repo.updateEvent(eventId, event, ticketInfor, new EventEditorRepository.Callback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(String message) {
                if (callback != null) callback.onFailure(message);
            }
        });
    }
}
