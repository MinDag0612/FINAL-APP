package com.FinalProject.feature_home_organizer.domain;

import android.util.Log;

import com.FinalProject.core.model.Events;
import com.FinalProject.feature_home_organizer.data.CreateEventRepositoryImpl;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateEventUseCase {
    public interface CreateEventCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private CreateEventRepositoryImpl repo = new CreateEventRepositoryImpl();

    public void excute(Events newEvent, CreateEventCallback callback) {
        if (newEvent == null) {
            callback.onFailure("Event is null");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        try{
            Date startDate = sdf.parse(newEvent.getEvent_start());
            Date endDate = sdf.parse(newEvent.getEvent_end());

            if (startDate.after(endDate)) {
                callback.onFailure("Start date must be before end date");
                return;
            }
            else {
                repo.createEvent(newEvent);
                callback.onSuccess();
            }

        }
        catch(Exception e){
            e.printStackTrace();
            Log.d("CreateEventUseCase", "excute: " + e);
        }
    }


}
