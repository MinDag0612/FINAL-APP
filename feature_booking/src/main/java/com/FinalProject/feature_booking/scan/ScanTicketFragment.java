package com.FinalProject.feature_booking.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.Order_API;
import com.FinalProject.feature_booking.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;

// ZXing
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanTicketFragment extends Fragment {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isScanning = false;

    public ScanTicketFragment() {
        super(R.layout.fragment_scan_ticket);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        previewView = root.findViewById(R.id.preview_view);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else if (isAdded()) {
                Snackbar.make(requireView(),
                        "Ứng dụng cần quyền camera để quét mã QR.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                if (isAdded()) {
                    Snackbar.make(requireView(),
                            "Không thể khởi động camera.",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        if (previewView != null) {
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    analysis
            );
            isScanning = true;
        } catch (Exception e) {
            if (isAdded()) {
                Snackbar.make(requireView(),
                        "Lỗi bind camera.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (!isScanning) {
            image.close();
            return;
        }

        try {
            Result result = decodeImageProxy(image);
            if (result != null) {
                // Dừng scan để tránh quét nhiều lần
                isScanning = false;
                String rawText = result.getText();
                requireActivity().runOnUiThread(() -> onQrCodeScanned(rawText));
            }
        } catch (Exception ignored) {
        } finally {
            image.close();
        }
    }

    @Nullable
    private Result decodeImageProxy(@NonNull ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (planes.length < 1) return null;

        ByteBuffer yBuffer = planes[0].getBuffer();
        byte[] yData = new byte[yBuffer.remaining()];
        yBuffer.get(yData);

        int width = image.getWidth();
        int height = image.getHeight();

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
        );
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        try {
            return reader.decode(bitmap);
        } catch (Exception e) {
            return null;
        } finally {
            reader.reset();
        }
    }

    private void onQrCodeScanned(@NonNull String rawText) {
        // rawText là JSON do app generate:
        // {"ticketId":"...", "event":"...", "summary":"...", "show":"...", ...}
        String ticketId   = rawText;
        String eventTitle = "";
        String summary    = "";
        String showInfo   = "";

        try {
            if (rawText.trim().startsWith("{")) {
                JSONObject obj = new JSONObject(rawText);
                ticketId   = obj.optString("ticketId", rawText);
                eventTitle = obj.optString("event", "");
                summary    = obj.optString("summary", "");
                showInfo   = obj.optString("show", "");
            }
        } catch (Exception ignored) {
        }

        if (TextUtils.isEmpty(ticketId)) {
            showResultAndFinish(
                    "INVALID",
                    "QR không hợp lệ.",
                    "",
                    "",
                    "",
                    ""
            );
            return;
        }

        verifyTicket(ticketId, eventTitle, summary, showInfo);
    }

    // ================== Verify ticket – gọi qua Order_API (core) ==================

    private void verifyTicket(@NonNull String ticketId,
                              @NonNull String eventTitle,
                              @NonNull String summary,
                              @NonNull String showInfo) {

        Order_API.getOrderById(ticketId)
                .addOnSuccessListener(orderDoc -> {
                    if (orderDoc == null || !orderDoc.exists()) {
                        showResultAndFinish(
                                "NOT_FOUND",
                                "Không tìm thấy vé trong hệ thống.",
                                ticketId,
                                eventTitle,
                                summary,
                                showInfo
                        );
                        return;
                    }

                    Boolean isPaid    = orderDoc.getBoolean(StoreField.OrderFields.IS_PAID);
                    Boolean checkedIn = orderDoc.getBoolean("checked_in");

                    if (isPaid == null || !isPaid) {
                        showResultAndFinish(
                                "UNPAID",
                                "Vé này chưa được thanh toán.",
                                ticketId,
                                eventTitle,
                                summary,
                                showInfo
                        );
                        return;
                    }

                    if (checkedIn != null && checkedIn) {
                        Timestamp at = orderDoc.getTimestamp("checked_in_at");
                        String timeStr = at != null ? at.toDate().toString() : "";
                        String message = "Vé đã được sử dụng"
                                + (timeStr.isEmpty() ? "" : (" lúc " + timeStr));
                        showResultAndFinish(
                                "ALREADY_USED",
                                message,
                                ticketId,
                                eventTitle,
                                summary,
                                showInfo
                        );
                        return;
                    }

                    // Hợp lệ & chưa check-in -> mark checked_in
                    Order_API.markOrderCheckedIn(ticketId)
                            .addOnSuccessListener(unused -> showResultAndFinish(
                                    "OK",
                                    "Check-in thành công.",
                                    ticketId,
                                    eventTitle,
                                    summary,
                                    showInfo
                            ))
                            .addOnFailureListener(e -> showResultAndFinish(
                                    "SERVER_ERROR",
                                    "Không thể cập nhật trạng thái vé.",
                                    ticketId,
                                    eventTitle,
                                    summary,
                                    showInfo
                            ));
                })
                .addOnFailureListener(e -> showResultAndFinish(
                        "SERVER_ERROR",
                        "Lỗi khi đọc dữ liệu vé.",
                        ticketId,
                        eventTitle,
                        summary,
                        showInfo
                ));
    }

    // ================== Điều hướng sang ScanResultFragment ==================

    private void showResultAndFinish(@NonNull String status,
                                     @NonNull String message,
                                     @NonNull String ticketId,
                                     @NonNull String eventTitle,
                                     @NonNull String summary,
                                     @NonNull String showInfo) {

        if (!isAdded()) return;

        Bundle b = new Bundle();
        b.putString("status", status);
        b.putString("statusMessage", message);
        b.putString("ticketId", ticketId);
        b.putString("eventTitle", eventTitle);
        b.putString("summary", summary);
        b.putString("showInfo", showInfo);

        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.scanResultFragment, b);
        } catch (Exception e) {
            Snackbar.make(requireView(),
                    "Không thể mở kết quả quét.",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isScanning = false;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
    }
}
