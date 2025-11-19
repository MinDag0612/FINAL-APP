package com.FinalProject.feature_home_organizer.data;

import com.FinalProject.core.util.Event_API;
import com.google.android.gms.tasks.Task;
import com.FinalProject.core.model.Events;
import com.google.firebase.firestore.DocumentReference;

public class CreateEventRepositoryImpl {
    public Task<DocumentReference> createEvent(Events event) {
        return Event_API.addNewEvent(event);
    }
}
