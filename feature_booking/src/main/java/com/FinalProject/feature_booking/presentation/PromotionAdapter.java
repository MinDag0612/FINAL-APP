package com.FinalProject.feature_booking.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.model.Promotion;
import com.FinalProject.feature_booking.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    private final List<Promotion> promotions;
    private final OnPromotionActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnPromotionActionListener {
        void onEdit(Promotion promotion);
        void onDelete(Promotion promotion, int position);
        void onToggleActive(Promotion promotion, int position);
    }

    public PromotionAdapter(List<Promotion> promotions, OnPromotionActionListener listener) {
        this.promotions = promotions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        holder.bind(promotions.get(position), position);
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    class PromotionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCode;
        private final TextView tvDescription;
        private final TextView tvDiscount;
        private final TextView tvValidPeriod;
        private final TextView tvUsage;
        private final Switch switchActive;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_promotion_code);
            tvDescription = itemView.findViewById(R.id.tv_promotion_description);
            tvDiscount = itemView.findViewById(R.id.tv_promotion_discount);
            tvValidPeriod = itemView.findViewById(R.id.tv_promotion_valid_period);
            tvUsage = itemView.findViewById(R.id.tv_promotion_usage);
            switchActive = itemView.findViewById(R.id.switch_promotion_active);
            btnEdit = itemView.findViewById(R.id.btn_edit_promotion);
            btnDelete = itemView.findViewById(R.id.btn_delete_promotion);
        }

        void bind(Promotion promotion, int position) {
            tvCode.setText(promotion.getPromotion_code());
            tvDescription.setText(promotion.getDescription());
            
            // Discount value
            String discountText;
            if ("percentage".equalsIgnoreCase(promotion.getPromotion_type())) {
                discountText = "Giảm " + promotion.getDiscount_value() + "%";
            } else {
                discountText = "Giảm " + promotion.getDiscount_value() + "đ";
            }
            tvDiscount.setText(discountText);

            // Valid period
            try {
                String from = dateFormat.format(new Date(promotion.getValid_from()));
                String to = dateFormat.format(new Date(promotion.getValid_until()));
                tvValidPeriod.setText("Từ " + from + " đến " + to);
            } catch (Exception e) {
                tvValidPeriod.setText("");
            }

            // Usage
            tvUsage.setText("Đã dùng: " + promotion.getUsage_count() + "/" + promotion.getUsage_limit());

            // Active switch
            switchActive.setChecked(promotion.is_active());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleActive(promotion, position);
                }
            });

            // Edit button
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(promotion);
                }
            });

            // Delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(promotion, position);
                }
            });
        }
    }
}
