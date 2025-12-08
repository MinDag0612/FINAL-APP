package com.FinalProject.feature_event_detail.model;

/**
 * UI model mô tả kết quả dự báo thời tiết cho sự kiện ngoài trời.
 */
public class WeatherForecast {

    public enum Condition {
        SUNNY,
        CLOUDY,
        RAINY,
        STORMY
    }

    private final Condition condition;
    private final String summary;
    private final int temperatureC;
    private final int rainChance;
    private final String recommendation;
    private final String timeWindow;

    public WeatherForecast(Condition condition,
                           String summary,
                           int temperatureC,
                           int rainChance,
                           String recommendation,
                           String timeWindow) {
        this.condition = condition;
        this.summary = summary;
        this.temperatureC = temperatureC;
        this.rainChance = rainChance;
        this.recommendation = recommendation;
        this.timeWindow = timeWindow;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getSummary() {
        return summary;
    }

    public int getTemperatureC() {
        return temperatureC;
    }

    public int getRainChance() {
        return rainChance;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getTimeWindow() {
        return timeWindow;
    }
}