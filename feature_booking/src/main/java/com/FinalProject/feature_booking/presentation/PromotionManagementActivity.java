package com.FinalProject.feature_booking.presentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.model.Promotion;
import com.FinalProject.core.util.Promotion_API;
import com.FinalProject.feature_booking.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity quản lý Promotions - Admin only
 * Hiển thị danh sách mã giảm giá, cho phép thêm/sửa/xóa
 */
public class PromotionManagementActivity extends AppCompatActivity {

    private RecyclerView rvPromotions;
    private FloatingActionButton fabAdd;
    private PromotionAdapter adapter;
    private List<Promotion> promotions = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_management);

        initViews();
        setupRecyclerView();
        loadPromotions();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_promotion_management);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý mã giảm giá");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvPromotions = findViewById(R.id.rv_promotions);
        fabAdd = findViewById(R.id.fab_add_promotion);

        fabAdd.setOnClickListener(v -> {
            // TODO: Mở dialog/activity tạo promotion mới
            // Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new PromotionAdapter(promotions, new PromotionAdapter.OnPromotionActionListener() {
            @Override
            public void onEdit(Promotion promotion) {
                // Toast.makeText(PromotionManagementActivity.this, "Edit: " + promotion.getPromotion_code(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(Promotion promotion, int position) {
                deletePromotion(promotion, position);
            }

            @Override
            public void onToggleActive(Promotion promotion, int position) {
                toggleActive(promotion, position);
            }
        });
        rvPromotions.setLayoutManager(new LinearLayoutManager(this));
        rvPromotions.setAdapter(adapter);
    }

    private void loadPromotions() {
        Promotion_API.getActivePromotions().addOnSuccessListener(querySnapshot -> {
            promotions.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Promotion promo = doc.toObject(Promotion.class);
                if (promo != null) {
                    promo.setPromotion_id(doc.getId());
                    promotions.add(promo);
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            // Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void deletePromotion(Promotion promotion, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa mã giảm giá")
            .setMessage("Bạn có chắc muốn xóa mã " + promotion.getPromotion_code() + "?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("Promotions")
                    .document(promotion.getPromotion_id())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        promotions.remove(position);
                        adapter.notifyItemRemoved(position);
                        // Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void toggleActive(Promotion promotion, int position) {
        boolean newStatus = !promotion.is_active();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("Promotions")
            .document(promotion.getPromotion_id())
            .update("is_active", newStatus)
            .addOnSuccessListener(aVoid -> {
                promotion.setIs_active(newStatus);
                adapter.notifyItemChanged(position);
                // Toast.makeText(this, newStatus ? "Đã kích hoạt" : "Đã vô hiệu hóa", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                // Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
