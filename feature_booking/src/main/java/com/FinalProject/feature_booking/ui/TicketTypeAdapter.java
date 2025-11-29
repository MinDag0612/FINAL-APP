package com.FinalProject.feature_booking.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.FinalProject.core.model.TicketInfor;
import com.FinalProject.feature_booking.R;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TicketTypeAdapter extends RecyclerView.Adapter<TicketTypeAdapter.TicketTypeVH> {

    // typeId = tickets_class, unitPrice = tickets_price
    public interface Listener {
        void onChangeQuantity(String typeId, long unitPrice, int delta);
    }

    private final Listener listener;
    private final List<TicketInfor> data = new ArrayList<>();
    private final NumberFormat vnd = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TicketTypeAdapter(@NonNull Listener l) {
        this.listener = l;
    }

    public void submit(List<TicketInfor> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketTypeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_type, parent, false);
        return new TicketTypeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketTypeVH h, int position) {
        TicketInfor t = data.get(position);

        // Name = tickets_class (VD: "Premium", "VIP", "General")
        h.tvName.setText(t.getTickets_class());

        // Price = tickets_price
        long price = t.getTickets_price();
        h.tvPrice.setText(vnd.format(price));

        h.btnPlus.setOnClickListener(v ->
                listener.onChangeQuantity(t.getTickets_class(), price, +1)
        );
        h.btnMinus.setOnClickListener(v ->
                listener.onChangeQuantity(t.getTickets_class(), price, -1)
        );
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class TicketTypeVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvPrice;
        final MaterialButton btnPlus, btnMinus;

        TicketTypeVH(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus= itemView.findViewById(R.id.btnMinus);
        }
    }
}
