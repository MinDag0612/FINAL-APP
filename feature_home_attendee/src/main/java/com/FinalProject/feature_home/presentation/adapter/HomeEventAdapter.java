package com.FinalProject.feature_home.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

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
    public interface EventShareListener {
        void onShareClick(HomeEvent event);
    }

    private final List<HomeEvent> items = new ArrayList<>();
    private final EventClickListener clickListener;
    private final EventShareListener shareListener;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // D:/MobiApp/FINAL3/feature_home_attendee/src/main/java/com/FinalProject/feature_home/presentation/adapter/HomeEventAdapter.java

    public HomeEventAdapter(EventClickListener clickListener, EventShareListener shareListener) {
        this.clickListener = clickListener;this.shareListener = shareListener;
        inputFormat.setLenient(false);
    }



    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        HomeEvent event = items.get(position);
        holder.bind(event, clickListener, shareListener);
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

        private final ImageView ivEventImage;
        private final TextView tvEventName;
        private final TextView tvEventInfo;
        private final TextView tvEventPrice;
        private final ImageButton btnShare;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            ivEventImage = itemView.findViewById(R.id.iv_event_image);
            tvEventName = itemView.findViewById(R.id.tv_event_name);
            tvEventInfo = itemView.findViewById(R.id.tv_event_info);
            tvEventPrice = itemView.findViewById(R.id.tv_event_price);
            btnShare = itemView.findViewById(R.id.btn_share_event);
        }

        void bind(HomeEvent event, EventClickListener clickListener, EventShareListener shareListener) {
            tvEventName.setText(event.getName());
            String date = formatDate(event.getStartTimeIso());
            String location = event.getLocation();
            tvEventInfo.setText(String.format("%s • %s", date, location));

            if (event.getStartingPrice() > 0) {
                tvEventPrice.setText("Từ " + currencyFormat.format(event.getStartingPrice()));
                itemView.findViewById(R.id.layout_price).setVisibility(View.VISIBLE);
            } else {
                itemView.findViewById(R.id.layout_price).setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onEventClick(event);
                }
            });

            btnShare.setOnClickListener(v -> {
                if (shareListener != null) {
                    shareListener.onShareClick(event);
                }
            });
        }

        private String formatDate(String raw) {
            if (raw == null || raw.isEmpty()) {
                return "Sắp diễn ra";
            }
            try {
                Date parsedDate = inputFormat.parse(raw);
                if (parsedDate != null) {
                    return outputFormat.format(parsedDate);
                }
            } catch (ParseException ignored) {
                return raw.split("T")[0];
            }
            return raw;
        }
    }
}
