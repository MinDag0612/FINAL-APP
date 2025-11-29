package com.FinalProject.feature_booking.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.model.Showtime;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.ShowtimeVH> {

    public interface Listener { void onShowtimeClicked(Showtime s); }

    private final Listener listener;
    private final List<Showtime> data = new ArrayList<>();

    public ShowtimeAdapter(Listener l){ this.listener = l; }

    public void submit(List<Showtime> list){
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ShowtimeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_showtime, parent, false);
        return new ShowtimeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowtimeVH h, int i){
        Showtime s = data.get(i);
        h.tv.setText(s.getTime());
        h.itemView.setOnClickListener(v -> listener.onShowtimeClicked(s));
    }

    @Override public int getItemCount(){ return data.size(); }

    static class ShowtimeVH extends RecyclerView.ViewHolder {
        final TextView tv;
        ShowtimeVH(@NonNull View itemView){
            super(itemView);
            tv = itemView.findViewById(R.id.tv);
        }
    }
}
