package com.FinalProject.feature_home_organizer.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_home_organizer.R;
import com.FinalProject.feature_home_organizer.data.OrganizerEventRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.EventVH> {

    public interface Listener {
        void onEdit(String eventId);
        void onCheckin(String eventId);
    }

    private final List<OrganizerEventRepository.EventItem> items = new ArrayList<>();
    private final Listener listener;

    public OrganizerEventAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrganizerEventRepository.EventItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_organizer_event_card, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class EventVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvStatus, tvStats;
        MaterialButton btnCheckin, btnEdit;

        EventVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvStatus = itemView.findViewById(R.id.tv_event_status);
            tvStats = itemView.findViewById(R.id.tv_event_stats);
            btnCheckin = itemView.findViewById(R.id.btn_checkin);
            btnEdit = itemView.findViewById(R.id.btn_edit_event);
        }

        void bind(OrganizerEventRepository.EventItem item) {
            tvTitle.setText(item.name != null ? item.name : "Sự kiện");
            String time = (item.start != null ? item.start : "") + (item.end != null ? " - " + item.end : "");
            tvTime.setText(time.trim());
            tvStatus.setText(item.type != null ? item.type : "Đang hoạt động");
            tvStats.setText(item.location != null ? item.location : "");

            btnCheckin.setOnClickListener(v -> {
                if (listener != null) listener.onCheckin(item.id);
            });
            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item.id);
            });
        }
    }
}
