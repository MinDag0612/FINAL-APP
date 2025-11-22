package com.FinalProject.feature_attendee_manager_organizer.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_attendee_manager_organizer.R;
import com.FinalProject.feature_attendee_manager_organizer.data.AttendeeRepository;

import java.util.ArrayList;
import java.util.List;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.AttendeeVH> {
    private final List<AttendeeRepository.AttendeeItem> items = new ArrayList<>();

    public void submit(List<AttendeeRepository.AttendeeItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendeeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false);
        return new AttendeeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendeeVH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AttendeeVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvTickets;
        AttendeeVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_attendee_name);
            tvEmail = itemView.findViewById(R.id.tv_attendee_email);
            tvTickets = itemView.findViewById(R.id.tv_attendee_tickets);
        }

        void bind(AttendeeRepository.AttendeeItem item) {
            tvName.setText(item.name != null && !item.name.isEmpty() ? item.name : "Người dùng");
            tvEmail.setText(item.email != null ? item.email : "");
            tvTickets.setText("Vé: " + item.totalTickets + " | Tổng: " + item.totalPrice);
        }
    }
}
