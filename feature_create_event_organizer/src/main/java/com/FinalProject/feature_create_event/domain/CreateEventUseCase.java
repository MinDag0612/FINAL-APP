package com.FinalProject.feature_create_event.domain;

import android.util.Log;

import com.FinalProject.core.model.Events;
import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_create_event.data.CreateEventRepositoryImpl;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateEventUseCase {
    public interface CreateEventCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private CreateEventRepositoryImpl repo = new CreateEventRepositoryImpl();

    public void execute(Events newEvent, TicketInfor ticketInfor, CreateEventCallback callback) {
        if (newEvent == null || ticketInfor == null) {
            if (callback != null) callback.onFailure("Thiếu dữ liệu sự kiện hoặc vé");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        try {
            Date startDate = sdf.parse(newEvent.getEvent_start());
            Date endDate = sdf.parse(newEvent.getEvent_end());

            if (startDate.after(endDate)) {
                if (callback != null) callback.onFailure("Giờ bắt đầu phải trước giờ kết thúc");
                return;
            }

            if (newEvent.getBase_price() <= 0 || ticketInfor.getTickets_price() <= 0) {
                if (callback != null) callback.onFailure("Giá vé phải lớn hơn 0");
                return;
            }

            repo.createEventWithTicket(newEvent, ticketInfor, new CreateEventRepositoryImpl.Callback() {
                @Override
                public void onSuccess() {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onFailure(String message) {
                    if (callback != null) callback.onFailure(message);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("CreateEventUseCase", "execute: " + e);
            if (callback != null) callback.onFailure("Lỗi định dạng thời gian");
        }
    }
}
