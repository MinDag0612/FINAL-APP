package com.FinalProject.feature_booking.presentation;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.FinalProject.feature_booking.data.BookingRepository;

public class BookingViewModelFactory implements ViewModelProvider.Factory {

    private final BookingRepository repo;

    public BookingViewModelFactory(@NonNull BookingRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(BookingViewModel.class)) {
            return (T) new BookingViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}
