package com.FinalProject.feature_attendee_manager_organizer.domain;

import android.content.Context;
import android.content.ContentValues;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.os.Build;
import android.provider.MediaStore;

import com.FinalProject.feature_attendee_manager_organizer.data.AttendeeRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportAttendeesUseCase {

    public interface Callback {
        void onSuccess(File file, String publicPath);
        void onFailure(String message);
    }

    public void exportCsv(Context context, List<AttendeeRepository.AttendeeItem> attendees, Callback callback) {
        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = context.getCacheDir();
            File file = new File(dir, "attendees.csv");
            StringBuilder sb = new StringBuilder();
            sb.append("Name,Email,Total Tickets,Total Price\n");
            for (AttendeeRepository.AttendeeItem a : attendees) {
                sb.append(safe(a.name)).append(",");
                sb.append(safe(a.email)).append(",");
                sb.append(a.totalTickets).append(",");
                sb.append(a.totalPrice).append("\n");
            }
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();

            String publicPath = writeToDownloads(context, bytes, "text/csv", "attendees.csv");
            callback.onSuccess(file, publicPath);
        } catch (IOException e) {
            callback.onFailure(e.getMessage());
        }
    }

    public void exportPdf(Context context, List<AttendeeRepository.AttendeeItem> attendees, Callback callback) {
        try {
            PdfDocument document = new PdfDocument();
            int pageNumber = 1;
            int y = 50;
            int pageWidth = 595;
            int pageHeight = 842;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setTextSize(14);
            paint.setColor(android.graphics.Color.BLACK);
            canvas.drawText("Danh sách khách tham dự", 40, y, paint);
            y += 30;
            paint.setTextSize(12);

            for (AttendeeRepository.AttendeeItem a : attendees) {
                if (y > pageHeight - 40) {
                    document.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 40;
                }
                canvas.drawText("Tên: " + safe(a.name), 40, y, paint);
                y += 18;
                canvas.drawText("Email: " + safe(a.email), 40, y, paint);
                y += 18;
                canvas.drawText("Vé: " + a.totalTickets + " | Tổng: " + a.totalPrice, 40, y, paint);
                y += 30;
            }
            document.finishPage(page);

            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = context.getCacheDir();
            File file = new File(dir, "attendees.pdf");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.writeTo(baos);
            byte[] bytes = baos.toByteArray();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            document.close();

            String publicPath = writeToDownloads(context, bytes, "application/pdf", "attendees.pdf");
            callback.onSuccess(file, publicPath);
        } catch (IOException e) {
            callback.onFailure(e.getMessage());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }

    private String writeToDownloads(Context context, byte[] bytes, String mime, String filename) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                android.net.Uri uri = context.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            os.write(bytes);
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                    return "Download/" + filename;
                }
            } else {
                File pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (pubDir != null) {
                    if (!pubDir.exists()) pubDir.mkdirs();
                    File outFile = new File(pubDir, filename);
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(bytes);
                        fos.flush();
                        return outFile.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
