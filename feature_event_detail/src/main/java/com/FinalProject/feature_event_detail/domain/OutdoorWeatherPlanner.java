package com.FinalProject.feature_event_detail.domain;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.FinalProject.feature_event_detail.model.EventDetail;
import com.FinalProject.feature_event_detail.model.WeatherForecast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dự đoán thời tiết dựa trên địa điểm & thời gian của sự kiện ngoài trời.
 * Sử dụng WeatherAPI (https://www.weatherapi.com/) cho dữ liệu thật.
 */
public class OutdoorWeatherPlanner {

    public interface ForecastCallback {
        void onSuccess(WeatherForecast forecast);

        void onError(String message);
    }

    private static final String TAG = "OutdoorWeatherPlanner";
    private static final String WEATHER_API_KEY = "f65a5278ebdd476091b95808250812";
    private static final String WEATHER_BASE_URL = "https://api.weatherapi.com/v1/forecast.json";
    private static final List<String> OUTDOOR_KEYWORDS = Arrays.asList(
            "ngoài trời", "outdoor", "sân", "stadium", "park", "beach", "quảng trường", "hồ", "garden");

    private final SimpleDateFormat isoFormatter;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public OutdoorWeatherPlanner() {
        isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Kiểm tra nhanh sự kiện có phải diễn ra ngoài trời hay không dựa trên eventType, tags và địa điểm.
     */
    public boolean isOutdoorEvent(EventDetail detail) {
        if (detail == null) {
            return false;
        }
        String location = safeLower(detail.getLocation());
        String eventType = safeLower(detail.getEventType());

        for (String keyword : OUTDOOR_KEYWORDS) {
            if (location.contains(keyword) || eventType.contains(keyword)) {
                return true;
            }
        }

        for (String tag : detail.getTags()) {
            if (safeLower(tag).contains("outdoor") || safeLower(tag).contains("ngoài trời")) {
                return true;
            }
        }
        return false;
    }

    public void fetchForecast(EventDetail detail, ForecastCallback callback) {
        if (detail == null || TextUtils.isEmpty(detail.getWeatherLocation())) {
            notifyError(callback, "Địa điểm sự kiện chưa sẵn sàng để dự báo.");
            return;
        }

        executorService.execute(() -> {
            try {
                WeatherForecast forecast = buildForecastFromApi(detail);
                if (forecast != null) {
                    notifySuccess(callback, forecast);
                } else {
                    notifyError(callback, "Không lấy được dữ liệu thời tiết từ WeatherAPI.");
                }
            } catch (IllegalArgumentException iae) {
                Log.w(TAG, "fetchForecast location error", iae);
                notifyError(callback, iae.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "fetchForecast error", e);
                notifyError(callback, "Không thể kết nối WeatherAPI, thử lại sau.");
            }
        });
    }

    private void notifySuccess(ForecastCallback callback, WeatherForecast forecast) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onSuccess(forecast));
    }

    private void notifyError(ForecastCallback callback, String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onError(message));
    }

    private WeatherForecast buildForecastFromApi(EventDetail detail) throws Exception {
        String encodedLocation = URLEncoder.encode(buildLocationQuery(detail), "UTF-8");
        String targetDate = extractDate(detail.getStartTimeIso());
        String dateParam = TextUtils.isEmpty(targetDate) ? "" : "&dt=" + targetDate;
        String requestUrl = WEATHER_BASE_URL + "?key=" + WEATHER_API_KEY + "&q=" + encodedLocation
                + "&days=3&aqi=no&alerts=no" + dateParam;

        HttpURLConnection connection = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("WeatherAPI response code: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return parseForecast(builder.toString(), detail);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private WeatherForecast parseForecast(String json, EventDetail detail) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject error = root.optJSONObject("error");
        if (error != null) {
            String message = error.optString("message", "Địa điểm chưa rõ ràng, vui lòng cập nhật cụ thể hơn.");
            throw new IllegalArgumentException("WeatherAPI: " + message);
        }
        JSONObject location = root.optJSONObject("location");
        String timezoneId = location != null ? location.optString("tz_id", TimeZone.getDefault().getID())
                : TimeZone.getDefault().getID();

        JSONObject forecastRoot = root.optJSONObject("forecast");
        if (forecastRoot == null) {
            return null;
        }
        JSONArray days = forecastRoot.optJSONArray("forecastday");
        if (days == null || days.length() == 0) {
            return null;
        }

        String targetDate = extractDate(detail.getStartTimeIso());
        int targetHour = extractHour(detail.getStartTimeIso());
        JSONObject dayObject = pickDay(days, targetDate);
        if (dayObject == null) {
            dayObject = days.optJSONObject(0);
        }
        if (dayObject == null) {
            return null;
        }

        JSONObject selectedHour = pickHour(dayObject.optJSONArray("hour"), targetHour);
        JSONObject conditionObject = null;
        double tempC = Double.NaN;
        int rainChance = 0;
        if (selectedHour != null) {
            tempC = selectedHour.optDouble("temp_c", Double.NaN);
            rainChance = selectedHour.optInt("chance_of_rain", 0);
            conditionObject = selectedHour.optJSONObject("condition");
        }

        JSONObject daySummary = dayObject.optJSONObject("day");
        if (Double.isNaN(tempC) && daySummary != null) {
            tempC = daySummary.optDouble("avgtemp_c", 0);
        }
        if (rainChance == 0 && daySummary != null) {
            rainChance = Math.max(daySummary.optInt("daily_chance_of_rain", 0), rainChance);
            if (conditionObject == null) {
                conditionObject = daySummary.optJSONObject("condition");
            }
        }

        String conditionText = conditionObject != null ? conditionObject.optString("text", "") : "";
        WeatherForecast.Condition condition = mapCondition(conditionText, rainChance);
        String summary = !TextUtils.isEmpty(conditionText) ? conditionText : buildSummary(condition);
        String recommendation = buildRecommendation(condition, rainChance, (int) Math.round(tempC));
        String timeWindow = formatTimeWindow(detail.getStartTimeIso(), detail.getEndTimeIso(), timezoneId,
                dayObject.optString("date"));

        return new WeatherForecast(
                condition,
                summary,
                (int) Math.round(tempC),
                rainChance,
                recommendation,
                timeWindow
        );
    }

    private JSONObject pickDay(JSONArray days, String targetDate) {
        if (days == null) {
            return null;
        }
        for (int i = 0; i < days.length(); i++) {
            JSONObject day = days.optJSONObject(i);
            if (day == null) {
                continue;
            }
            if (TextUtils.isEmpty(targetDate) || targetDate.equals(day.optString("date"))) {
                return day;
            }
        }
        return null;
    }

    private JSONObject pickHour(JSONArray hours, int targetHour) {
        if (hours == null || hours.length() == 0) {
            return null;
        }
        if (targetHour < 0) {
            return hours.optJSONObject(Math.min(12, hours.length() - 1));
        }
        JSONObject fallback = hours.optJSONObject(Math.min(12, hours.length() - 1));
        for (int i = 0; i < hours.length(); i++) {
            JSONObject hour = hours.optJSONObject(i);
            if (hour == null) {
                continue;
            }
            String time = hour.optString("time");
            int parsedHour = extractHourFromTimeString(time);
            if (parsedHour == targetHour) {
                return hour;
            }
            if (fallback == null && parsedHour > targetHour) {
                fallback = hour;
            }
        }
        return fallback;
    }

    private String buildLocationQuery(EventDetail detail) {
        String location = detail.getWeatherLocation();
        String eventType = safeLower(detail.getEventType());
        String normalized = location != null ? location.trim() : "";

        boolean hasCountry = safeLower(normalized).contains("vietnam")
                || safeLower(normalized).contains("việt nam")
                || safeLower(normalized).contains("vn");
        if (!hasCountry) {
            normalized = normalized + ", Vietnam";
        }

        if (!TextUtils.isEmpty(eventType) && !safeLower(normalized).contains(eventType)) {
            normalized = eventType + ", " + normalized;
        }
        return normalized;
    }

    private int extractHourFromTimeString(String time) {
        if (TextUtils.isEmpty(time)) {
            return -1;
        }
        try {
            String[] parts = time.split(" ");
            if (parts.length < 2) {
                return -1;
            }
            String[] hourMinute = parts[1].split(":");
            return Integer.parseInt(hourMinute[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private WeatherForecast.Condition mapCondition(String text, int rainChance) {
        String lower = safeLower(text);
        if (lower.contains("thunder") || lower.contains("storm")) {
            return WeatherForecast.Condition.STORMY;
        }
        if (lower.contains("rain") || lower.contains("shower")) {
            return WeatherForecast.Condition.RAINY;
        }
        if (lower.contains("overcast") || lower.contains("cloud") || lower.contains("mist")
                || lower.contains("fog")) {
            return WeatherForecast.Condition.CLOUDY;
        }
        if (rainChance >= 60) {
            return WeatherForecast.Condition.RAINY;
        }
        return WeatherForecast.Condition.SUNNY;
    }

    private String buildSummary(WeatherForecast.Condition condition) {
        switch (condition) {
            case SUNNY:
                return "Trời nắng nhẹ";
            case CLOUDY:
                return "Nhiều mây, thoáng mát";
            case RAINY:
                return "Có thể có mưa rào";
            case STORMY:
                return "Mưa giông rải rác";
            default:
                return "Thời tiết ổn định";
        }
    }

    private String buildRecommendation(WeatherForecast.Condition condition, int rainChance, int temperature) {
        if (rainChance >= 70 || condition == WeatherForecast.Condition.STORMY) {
            return "Mang áo mưa/ô, bọc thiết bị điện tử cẩn thận.";
        }
        if (condition == WeatherForecast.Condition.SUNNY && temperature >= 31) {
            return "Kem chống nắng, mũ và nước uống sẽ giúp bạn thoải mái hơn.";
        }
        if (condition == WeatherForecast.Condition.CLOUDY) {
            return "Không quá nóng, bạn có thể chọn trang phục thoải mái và mang thêm áo mỏng.";
        }
        if (condition == WeatherForecast.Condition.RAINY) {
            return "Mang theo áo khoác nhẹ và bao phủ đồ điện tử.";
        }
        return "Chuẩn bị áo khoác mỏng và đến sớm để chọn vị trí quan sát tốt.";
    }

    private String formatTimeWindow(String startIso, String endIso, String timezoneId, String fallbackDate) {
        String startFormatted = parseIso(startIso, timezoneId);
        String endFormatted = parseIso(endIso, timezoneId);
        if (TextUtils.isEmpty(startFormatted) && TextUtils.isEmpty(endFormatted)) {
            return TextUtils.isEmpty(fallbackDate) ? "Khung giờ sự kiện" : fallbackDate;
        }
        if (TextUtils.isEmpty(endFormatted)) {
            return startFormatted;
        }
        return startFormatted + " - " + endFormatted;
    }

    private String parseIso(String iso, String timezoneId) {
        if (TextUtils.isEmpty(iso)) {
            return "";
        }
        try {
            Date date = isoFormatter.parse(iso);
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm dd/MM z", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getTimeZone(timezoneId));
            return formatter.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat fallback = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date date = fallback.parse(iso.replace("T", " ").replace("Z", ""));
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
                formatter.setTimeZone(TimeZone.getTimeZone(timezoneId));
                return formatter.format(date);
            } catch (ParseException ignored) {
                return iso.replace("T", " ").replace("Z", "");
            }
        }
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.getDefault()) : "";
    }

    private String extractDate(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return "";
        }
        try {
            Date date = isoFormatter.parse(iso);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private int extractHour(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return -1;
        }
        try {
            Date date = isoFormatter.parse(iso);
            SimpleDateFormat hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
            hourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return Integer.parseInt(hourFormat.format(date));
        } catch (Exception e) {
            return -1;
        }
    }
}