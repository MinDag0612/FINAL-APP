package com.FinalProject.feature_home.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_home.R;
import com.FinalProject.feature_home.model.HomeEvent;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeEventAdapter extends RecyclerView.Adapter<HomeEventAdapter.EventViewHolder> {

    public interface EventClickListener {
        void onEventClick(HomeEvent event);
    }

    private final List<HomeEvent> items = new ArrayList<>();
    private final EventClickListener listener;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeEventAdapter(EventClickListener listener) {
        this.listener = listener;
        inputFormat.setLenient(false);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        HomeEvent event = items.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<HomeEvent> events) {
        items.clear();
        if (events != null) {
            items.addAll(events);
        }
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvType;
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView tvLocation;
        private final TextView tvPrice;
        private final ImageView ivBadge;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_event_type);
            tvName = itemView.findViewById(R.id.tv_event_name);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvPrice = itemView.findViewById(R.id.tv_event_price);
            ivBadge = itemView.findViewById(R.id.iv_event_badge);
        }

        void bind(HomeEvent event) {
            tvType.setText(event.getEventType());
            tvName.setText(event.getName());
            tvDate.setText(formatDate(event.getStartTimeIso()));
            tvLocation.setText(event.getLocation());
            tvPrice.setText(event.getStartingPrice() > 0
                    ? currencyFormat.format(event.getStartingPrice())
                    : itemView.getContext().getString(R.string.home_price_pending));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });

            ivBadge.setImageResource(getBadgeForType(event.getEventType()));
        }

        private String formatDate(String raw) {
            if (raw == null || raw.isEmpty()) {
                return itemView.getContext().getString(R.string.home_date_pending);
            }
            try {
                Date parsed = inputFormat.parse(raw);
                if (parsed != null) {
                    return outputFormat.format(parsed);
                }
            } catch (ParseException ignored) {
            }
            return raw;
        }

        private int getBadgeForType(String type) {
            if (type == null) return R.drawable.ic_event_music;
            String normalized = type.toLowerCase(Locale.ROOT);
            if (normalized.contains("concert") || normalized.contains("music")) {
                return R.drawable.ic_event_music;
            }
            if (normalized.contains("workshop")) {
                return R.drawable.ic_event_workshop;
            }
            if (normalized.contains("startup") || normalized.contains("business")) {
                return R.drawable.ic_event_business;
            }
            return R.drawable.ic_event_music;
        }
    }
}
