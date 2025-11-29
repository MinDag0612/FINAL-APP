package com.FinalProject.feature_booking.scan;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.feature_booking.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

public class ScanResultFragment extends Fragment {

    private ImageView ivStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusMessage;
    private TextView tvStatusMeta;
    private TextView tvStatusTicket;
    private MaterialButton btnScanAgain;
    private MaterialButton btnClose;

    public ScanResultFragment() {
        super(R.layout.fragment_scan_result);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        ivStatusIcon   = v.findViewById(R.id.iv_status_icon);
        tvStatusTitle  = v.findViewById(R.id.tv_status_title);
        tvStatusMessage= v.findViewById(R.id.tv_status_message);
        tvStatusMeta   = v.findViewById(R.id.tv_status_meta);
        tvStatusTicket = v.findViewById(R.id.tv_status_ticket);
        btnScanAgain   = v.findViewById(R.id.btn_scan_again);
        btnClose       = v.findViewById(R.id.btn_close_result);

        Bundle args = getArguments();
        String status        = "UNKNOWN";
        String statusMessage = "";
        String ticketId      = "";
        String eventTitle    = "";
        String summary       = "";
        String showInfo      = "";

        if (args != null) {
            status        = args.getString("status", "UNKNOWN");
            statusMessage = args.getString("statusMessage", "");
            ticketId      = args.getString("ticketId", "");
            eventTitle    = args.getString("eventTitle", "");
            summary       = args.getString("summary", "");
            showInfo      = args.getString("showInfo", "");
        }

        renderStatus(v, status, statusMessage, ticketId, eventTitle, summary, showInfo);

        if (btnScanAgain != null) {
            btnScanAgain.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).popBackStack()
            );
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).popBackStack()
            );
        }
    }

    private void renderStatus(@NonNull View root,
                              @NonNull String status,
                              @NonNull String statusMessage,
                              @NonNull String ticketId,
                              @NonNull String eventTitle,
                              @NonNull String summary,
                              @NonNull String showInfo) {

        String titleText;
        int iconRes;

        // attr dùng cho màu nền / chữ
        int bgAttr;
        int fgAttr;

        switch (status) {
            case "OK":
                titleText = "Vé hợp lệ";
                iconRes   = R.drawable.ic_check_circle_24;
                // màu “success”: secondaryContainer
                bgAttr = com.google.android.material.R.attr.colorSecondaryContainer;
                fgAttr = com.google.android.material.R.attr.colorOnSecondaryContainer;
                break;

            case "ALREADY_USED":
                titleText = "Vé đã sử dụng";
                iconRes   = R.drawable.ic_error_24;
                // màu “cảnh báo/lỗi”: surfaceVariant
                bgAttr = com.google.android.material.R.attr.colorSurfaceVariant;
                fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
                break;

            case "UNPAID":
                titleText = "Vé chưa thanh toán";
                iconRes   = R.drawable.ic_error_24;
                bgAttr = com.google.android.material.R.attr.colorSurfaceVariant;
                fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
                break;

            case "NOT_FOUND":
                titleText = "Không tìm thấy vé";
                iconRes   = R.drawable.ic_error_24;
                bgAttr = com.google.android.material.R.attr.colorSurfaceVariant;
                fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
                break;

            case "SERVER_ERROR":
            default:
                titleText = "Lỗi kiểm tra vé";
                iconRes   = R.drawable.ic_error_24;
                bgAttr = com.google.android.material.R.attr.colorSurfaceVariant;
                fgAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
                break;
        }

        int bgColor = MaterialColors.getColor(root, bgAttr);
        int fgColor = MaterialColors.getColor(root, fgAttr);

        // Icon + tint
        if (ivStatusIcon != null) {
            ivStatusIcon.setImageResource(iconRes);
            ivStatusIcon.setImageTintList(ColorStateList.valueOf(bgColor));
        }

        // Title
        if (tvStatusTitle != null) {
            tvStatusTitle.setText(titleText);
            tvStatusTitle.setTextColor(fgColor);
        }

        // Message
        if (tvStatusMessage != null) {
            if (TextUtils.isEmpty(statusMessage)) {
                tvStatusMessage.setText(status);
            } else {
                tvStatusMessage.setText(statusMessage);
            }
        }

        // Meta info: event name, seats, show
        if (tvStatusMeta != null) {
            StringBuilder meta = new StringBuilder();
            if (!TextUtils.isEmpty(eventTitle)) {
                meta.append(eventTitle).append("\n");
            }
            if (!TextUtils.isEmpty(summary)) {
                meta.append("Vé: ").append(summary).append("\n");
            }
            if (!TextUtils.isEmpty(showInfo)) {
                meta.append(showInfo);
            }
            tvStatusMeta.setText(meta.toString().trim());
        }

        // Ticket ID
        if (tvStatusTicket != null) {
            if (!TextUtils.isEmpty(ticketId)) {
                tvStatusTicket.setText("Ticket ID: " + ticketId);
                tvStatusTicket.setVisibility(View.VISIBLE);
            } else {
                tvStatusTicket.setText("");
                tvStatusTicket.setVisibility(View.GONE);
            }
        }
    }
}
