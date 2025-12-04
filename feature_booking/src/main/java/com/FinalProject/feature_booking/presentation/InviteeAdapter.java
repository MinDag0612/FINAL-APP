package com.FinalProject.feature_booking.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_booking.R;

import java.util.List;

public class InviteeAdapter extends RecyclerView.Adapter<InviteeAdapter.InviteeViewHolder> {

    private final List<GroupBookingActivity.Invitee> invitees;
    private final OnRemoveClickListener removeListener;

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    public InviteeAdapter(List<GroupBookingActivity.Invitee> invitees, OnRemoveClickListener listener) {
        this.invitees = invitees;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public InviteeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitee, parent, false);
        return new InviteeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteeViewHolder holder, int position) {
        holder.bind(invitees.get(position), position);
    }

    @Override
    public int getItemCount() {
        return invitees.size();
    }

    class InviteeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmail;
        private final TextView tvTickets;
        private final TextView tvStatus;
        private final ImageButton btnRemove;

        InviteeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tv_invitee_email);
            tvTickets = itemView.findViewById(R.id.tv_invitee_tickets);
            tvStatus = itemView.findViewById(R.id.tv_invitee_status);
            btnRemove = itemView.findViewById(R.id.btn_remove_invitee);
        }

        void bind(GroupBookingActivity.Invitee invitee, int position) {
            tvEmail.setText(invitee.email);
            tvTickets.setText(invitee.ticketCount + " vé");
            tvStatus.setText(getStatusText(invitee.status));

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemove(position);
                }
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case "CONFIRMED": return "✓ Đã xác nhận";
                case "CANCELLED": return "✗ Đã hủy";
                case "PENDING":
                default: return "⏳ Chờ xác nhận";
            }
        }
    }
}
