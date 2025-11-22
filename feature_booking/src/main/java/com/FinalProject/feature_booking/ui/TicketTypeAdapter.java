package com.FinalProject.feature_booking.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.FinalProject.feature_booking.R;
import com.FinalProject.feature_booking.model.TicketType;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TicketTypeAdapter extends RecyclerView.Adapter<TicketTypeAdapter.TicketTypeVH> {

    public interface Listener { void onChangeQuantity(String typeId, long unitPrice, int delta); }

    private final Listener listener;
    private final List<TicketType> data = new ArrayList<>();
    private final NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    public TicketTypeAdapter(Listener l){ this.listener = l; }

    public void submit(List<TicketType> list){
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public TicketTypeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_type, parent, false);
        return new TicketTypeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketTypeVH h, int i){
        TicketType t = data.get(i);
        h.tvName.setText(t.getName());
        h.tvPrice.setText(vnd.format(t.getPriceSafe()));
        h.btnPlus.setOnClickListener(v -> listener.onChangeQuantity(t.getTypeId(), t.getPriceSafe(), +1));
        h.btnMinus.setOnClickListener(v -> listener.onChangeQuantity(t.getTypeId(), t.getPriceSafe(), -1));
    }

    @Override public int getItemCount(){ return data.size(); }

    static class TicketTypeVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvPrice;
        final MaterialButton btnPlus, btnMinus;
        TicketTypeVH(@NonNull View itemView){
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus= itemView.findViewById(R.id.btnMinus);
        }
    }
}
