package com.FinalProject.feature_event_detail.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_booking.presentation.BookingNavigator;
import com.FinalProject.feature_event_detail.R;
import com.FinalProject.feature_event_detail.data.EventDetailRepository;
import com.FinalProject.feature_event_detail.data.MockEventDetailFactory;
import com.FinalProject.feature_event_detail.domain.OutdoorWeatherPlanner;
import com.FinalProject.feature_event_detail.model.WeatherForecast;
import com.FinalProject.feature_event_detail.domain.GetEventDetailUseCase;
import com.FinalProject.feature_event_detail.model.EventDetail;
import com.FinalProject.feature_event_detail.model.ReviewDisplayItem;
import com.FinalProject.feature_event_detail.model.TicketTier;
import com.FinalProject.feature_event_detail.model.TimelineItem;
import com.FinalProject.feature_review_event.presentation.ReviewEventNavigator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình hiển thị chi tiết một sự kiện bao gồm thông tin, hạng vé, lịch trình và review.
 */
public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_SCHEDULE = "extra_event_schedule";
    public static final String EXTRA_EVENT_LOCATION = "extra_event_location";
    public static final String EXTRA_EVENT_DESCRIPTION = "extra_event_description";
    public static final String EXTRA_EVENT_PRICE = "extra_event_price";
    public static final String EXTRA_TAGS = "extra_tags";
    public static final String EXTRA_TIMELINE = "extra_timeline";

    private CircularProgressIndicator progressIndicator;
    private ViewGroup contentContainer;
    private ViewGroup errorContainer;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private ImageView imgEventCover;
    private TextView tvEventType;
    private TextView tvEventTitle;
    private TextView tvEventSchedule;
    private TextView tvEventLocation;
    private TextView tvEventDescription;
    private TextView tvEventCast;
    private TextView tvRatingValue;
    private TextView tvRatingCount;
    private TextView tvTicketEmpty;
    private TextView tvTimelineEmpty;
    private TextView tvReviewsEmpty;
    private ChipGroup chipGroupTags;
    private LinearLayout ticketsContainer;
    private LinearLayout timelineContainer;
    private LinearLayout reviewsContainer;
    private MaterialButton btnChooseSeat;
    private MaterialButton btnWriteReview;
    private View cardWeather;
    private TextView tvWeatherSummary;
    private TextView tvWeatherTime;
    private TextView tvWeatherTemperature;
    private TextView tvWeatherRain;
    private TextView tvWeatherRecommendation;
    private TextView tvWeatherUnavailable;
    private ImageView imgWeatherIcon;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private GetEventDetailUseCase getEventDetailUseCase;
    private LayoutInflater inflater;
    private String eventId;
    private boolean hasBoundRemoteData = false;
    private EventDetail currentDetail;
    private EventDetail fallbackDetail;
    private OutdoorWeatherPlanner weatherPlanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_FeatureEventDetail);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        getEventDetailUseCase = new GetEventDetailUseCase(new EventDetailRepository());
        weatherPlanner = new OutdoorWeatherPlanner();
        inflater = LayoutInflater.from(this);

        bindViews();
        configureToolbar();
        applyFallbackFromIntent(getIntent());
        loadEventDetail();
    }

    private void bindViews() {
        progressIndicator = findViewById(R.id.progress_event_detail);
        contentContainer = findViewById(R.id.layout_event_content);
        errorContainer = findViewById(R.id.layout_event_error);
        tvErrorMessage = findViewById(R.id.tv_event_error_message);
        btnRetry = findViewById(R.id.btn_event_retry);

        imgEventCover = findViewById(R.id.img_event_cover);
        tvEventType = findViewById(R.id.tv_event_type);
        tvEventTitle = findViewById(R.id.tv_event_title);
        tvEventSchedule = findViewById(R.id.tv_event_schedule);
        tvEventLocation = findViewById(R.id.tv_event_location);
        tvEventDescription = findViewById(R.id.tv_event_description);
        tvEventCast = findViewById(R.id.tv_event_cast);
        tvRatingValue = findViewById(R.id.tv_event_rating_value);
        tvRatingCount = findViewById(R.id.tv_event_rating_count);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        ticketsContainer = findViewById(R.id.layout_ticket_container);
        timelineContainer = findViewById(R.id.layout_timeline_container);
        reviewsContainer = findViewById(R.id.layout_reviews_container);
        tvTicketEmpty = findViewById(R.id.tv_ticket_empty);
        tvTimelineEmpty = findViewById(R.id.tv_timeline_empty);
        tvReviewsEmpty = findViewById(R.id.tv_reviews_empty);
        btnChooseSeat = findViewById(R.id.btn_choose_seat);
        btnWriteReview = findViewById(R.id.btn_write_review);
        cardWeather = findViewById(R.id.card_weather);
        tvWeatherSummary = findViewById(R.id.tv_weather_summary);
        tvWeatherTime = findViewById(R.id.tv_weather_time);
        tvWeatherTemperature = findViewById(R.id.tv_weather_temperature);
        tvWeatherRain = findViewById(R.id.tv_weather_rain_chance);
        tvWeatherRecommendation = findViewById(R.id.tv_weather_recommendation);
        tvWeatherUnavailable = findViewById(R.id.tv_weather_unavailable);
        imgWeatherIcon = findViewById(R.id.img_weather_icon);

        // Back button
        View btnBack = findViewById(R.id.btn_back_event_detail);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        btnRetry.setOnClickListener(v -> loadEventDetail());
        btnChooseSeat.setOnClickListener(v -> openBooking());
        btnWriteReview.setOnClickListener(v -> openReviewScreen());
    }

    private void configureToolbar() {
        // Toolbar removed, using MaterialButton instead
    }

    private void applyFallbackFromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        android.util.Log.d("EventDetailActivity", "eventId from intent: " + eventId);
        setTextIfNotEmpty(tvEventTitle, intent.getStringExtra(EXTRA_EVENT_TITLE));
        setTextIfNotEmpty(tvEventSchedule, intent.getStringExtra(EXTRA_EVENT_SCHEDULE));
        setTextIfNotEmpty(tvEventLocation, intent.getStringExtra(EXTRA_EVENT_LOCATION));
        setTextIfNotEmpty(tvEventDescription, intent.getStringExtra(EXTRA_EVENT_DESCRIPTION));

        long fallbackPrice = intent.getLongExtra(EXTRA_EVENT_PRICE, 0);
        if (fallbackPrice > 0) {
            btnChooseSeat.setText(getString(R.string.event_detail_cta_with_price,
                    currencyFormat.format(fallbackPrice)));
        }

        ArrayList<String> fallbackTags = intent.getStringArrayListExtra(EXTRA_TAGS);
        if (fallbackTags != null && !fallbackTags.isEmpty()) {
            renderTags(fallbackTags);
        }

        ArrayList<String> fallbackTimeline = intent.getStringArrayListExtra(EXTRA_TIMELINE);
        List<TimelineItem> timelineItems = convertToTimelineItems(fallbackTimeline);
        if (!timelineItems.isEmpty()) {
            renderTimeline(timelineItems);
        }
        fallbackDetail = buildFallbackDetail(
                eventId,
                intent.getStringExtra(EXTRA_EVENT_TITLE),
                intent.getStringExtra(EXTRA_EVENT_DESCRIPTION),
                intent.getStringExtra(EXTRA_EVENT_LOCATION),
                intent.getStringExtra(EXTRA_EVENT_SCHEDULE),
                fallbackTags,
                timelineItems
        );
    }

    private void loadEventDetail() {
        if (TextUtils.isEmpty(eventId)) {
            bindDetail(MockEventDetailFactory.create());
            showErrorMessage(getString(R.string.event_detail_missing_id));
            return;
        }
        showLoading(true);
        getEventDetailUseCase.execute(eventId, new GetEventDetailUseCase.Callback() {
            @Override
            public void onSuccess(EventDetail detail) {
                showLoading(false);
                hasBoundRemoteData = true;
                bindDetail(detail);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                if (hasBoundRemoteData) {
                    Snackbar.make(contentContainer, message, Snackbar.LENGTH_LONG).show();
                } else {
                    EventDetail detailToShow = fallbackDetail != null
                            ? fallbackDetail
                            : MockEventDetailFactory.create();
                    bindDetail(detailToShow);
                    showErrorMessage(message != null
                            ? message
                            : getString(R.string.event_detail_missing_id));
                }
            }
        });
    }

    private List<TimelineItem> convertToTimelineItems(List<String> rawItems) {
        List<TimelineItem> items = new ArrayList<>();
        if (rawItems == null || rawItems.isEmpty()) {
            return items;
        }
        for (String raw : rawItems) {
            String[] parts = raw.split("\\|");
            String time = parts.length > 0 ? parts[0].trim() : "";
            String title = parts.length > 1 ? parts[1].trim() : "";
            String desc = parts.length > 2 ? parts[2].trim() : "";
            items.add(new TimelineItem(time, title, desc));
        }
        return items;
    }

    private EventDetail buildFallbackDetail(@Nullable String id,
                                            @Nullable String title,
                                            @Nullable String description,
                                            @Nullable String location,
                                            @Nullable String schedule,
                                            @Nullable List<String> tags,
                                            @Nullable List<TimelineItem> timelineItems) {
        String fallbackId = TextUtils.isEmpty(id) ? "fallback_event" : id;
        String fallbackTitle = !TextUtils.isEmpty(title)
                ? title
                : getString(R.string.event_detail_default_title);
        String fallbackLocation = !TextUtils.isEmpty(location)
                ? location
                : getString(R.string.event_detail_default_location);
        String fallbackDescription = description != null ? description : "";
        String fallbackEventType = tags != null && !tags.isEmpty()
                ? tags.get(0)
                : "";
        List<String> fallbackTags = tags != null
                ? new ArrayList<>(tags)
                : Collections.emptyList();
        List<TimelineItem> fallbackTimeline = timelineItems != null
                ? new ArrayList<>(timelineItems)
                : Collections.emptyList();

        return new EventDetail(
                fallbackId,
                fallbackTitle,
                fallbackDescription,
                fallbackLocation,
                fallbackLocation,
                fallbackEventType,
                schedule,
                null,
                null,
                null,
                fallbackTags,
                Collections.emptyList(),
                fallbackTimeline,
                Collections.emptyList(),
                0,
                0
        );
    }

    private void openReviewScreen() {
        if (TextUtils.isEmpty(eventId)) {
            // Toast.makeText(this, R.string.event_detail_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        String title = valueOf(tvEventTitle);
        String schedule = valueOf(tvEventSchedule);
        String location = valueOf(tvEventLocation);
        Intent intent = ReviewEventNavigator.createIntent(
                this,
                eventId,
                title,
                schedule,
                location
        );
        startActivity(intent);
    }

    private void openBooking() {
        String id = eventId;
        android.util.Log.d("EventDetailActivity", "========== OPEN BOOKING ==========");
        android.util.Log.d("EventDetailActivity", "openBooking - eventId: " + id);
        if (TextUtils.isEmpty(id)) {
            // Toast.makeText(this, R.string.event_detail_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        String title = valueOf(tvEventTitle);
        String location = valueOf(tvEventLocation);
        String schedule = valueOf(tvEventSchedule);
        long startingPrice = resolveStartingPrice();
        String showId = id + "_DEFAULT";
        android.util.Log.d("EventDetailActivity", "openBooking - creating BookingNavigator intent");
        android.util.Log.d("EventDetailActivity", "  -> eventId: " + id);
        android.util.Log.d("EventDetailActivity", "  -> title: " + title);
        android.util.Log.d("EventDetailActivity", "  -> location: " + location);
        android.util.Log.d("EventDetailActivity", "  -> schedule: " + schedule);
        android.util.Log.d("EventDetailActivity", "  -> price: " + startingPrice);
        android.util.Log.d("EventDetailActivity", "  -> showId: " + showId);
        Intent intent = BookingNavigator.createBookingIntent(
                this,
                id,
                title,
                location,
                schedule,
                startingPrice,
                showId
        );
        startActivity(intent);
        android.util.Log.d("EventDetailActivity", "========== BOOKING STARTED ==========");
    }

    private long resolveStartingPrice() {
        if (currentDetail != null && !currentDetail.getTicketTiers().isEmpty()) {
            return currentDetail.getTicketTiers().get(0).getPrice();
        }
        if (fallbackDetail != null && !fallbackDetail.getTicketTiers().isEmpty()) {
            return fallbackDetail.getTicketTiers().get(0).getPrice();
        }
        return 0;
    }

    private String valueOf(TextView view) {
        return view != null ? view.getText().toString() : null;
    }

    private void bindDetail(EventDetail detail) {
        contentContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);

        eventId = detail.getId();
        
        // Set banner làm ảnh sự kiện
        if (imgEventCover != null) {
            imgEventCover.setImageResource(R.drawable.banner);
        }

        tvEventTitle.setText(detail.getName());
        tvEventDescription.setText(detail.getDescription());
        setTextIfNotEmpty(tvEventLocation, detail.getLocation());
        setTextIfNotEmpty(tvEventType, detail.getEventType());
        if (!TextUtils.isEmpty(detail.getCast())) {
            tvEventCast.setText(getString(R.string.event_detail_cast_format, detail.getCast()));
            tvEventCast.setVisibility(View.VISIBLE);
        } else {
            tvEventCast.setVisibility(View.GONE);
        }

        String schedule = formatSchedule(detail.getStartTimeIso(), detail.getEndTimeIso());
        tvEventSchedule.setText(schedule);

        tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", detail.getAverageRating()));
        tvRatingCount.setText(getString(R.string.event_detail_review_count, detail.getReviewCount()));

        if (!detail.getTags().isEmpty()) {
            renderTags(detail.getTags());
        }

        renderTickets(detail.getTicketTiers());
        renderTimeline(detail.getTimelineItems());
        renderReviews(detail.getReviews());
        renderWeather(detail);

        long startingPrice = detail.getTicketTiers().isEmpty()
                ? 0
                : detail.getTicketTiers().get(0).getPrice();
        if (startingPrice > 0) {
            btnChooseSeat.setText(getString(R.string.event_detail_cta_with_price,
                    currencyFormat.format(startingPrice)));
        }
        currentDetail = detail;
    }

    private void renderTags(List<String> tags) {
        chipGroupTags.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            chipGroupTags.setVisibility(View.GONE);
            return;
        }
        chipGroupTags.setVisibility(View.VISIBLE);
        for (String tag : tags) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_detail_chip, chipGroupTags, false);
            chip.setText(tag);
            chipGroupTags.addView(chip);
        }
    }

    private void renderTickets(List<TicketTier> tiers) {
        ticketsContainer.removeAllViews();
        if (tiers == null || tiers.isEmpty()) {
            tvTicketEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvTicketEmpty.setVisibility(View.GONE);
        for (TicketTier tier : tiers) {
            View view = inflater.inflate(R.layout.item_ticket_tier, ticketsContainer, false);
            TextView tvLabel = view.findViewById(R.id.tv_ticket_label);
            TextView tvPrice = view.findViewById(R.id.tv_ticket_price);
            TextView tvAvailability = view.findViewById(R.id.tv_ticket_availability);

            tvLabel.setText(tier.getLabel());
            tvPrice.setText(currencyFormat.format(tier.getPrice()));

            int remaining = tier.getRemaining();
            tvAvailability.setText(remaining > 0
                    ? getString(R.string.event_detail_ticket_remaining, remaining)
                    : getString(R.string.event_detail_ticket_sold_out));
            tvAvailability.setActivated(remaining > 0);
            ticketsContainer.addView(view);
        }
    }

    private void renderTimeline(List<TimelineItem> timelineItems) {
        timelineContainer.removeAllViews();
        if (timelineItems == null || timelineItems.isEmpty()) {
            tvTimelineEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvTimelineEmpty.setVisibility(View.GONE);
        for (int i = 0; i < timelineItems.size(); i++) {
            TimelineItem item = timelineItems.get(i);
            View view = inflater.inflate(R.layout.view_timeline_item, timelineContainer, false);
            TextView tvTime = view.findViewById(R.id.tv_timeline_time);
            TextView tvTitle = view.findViewById(R.id.tv_timeline_title);
            TextView tvDescription = view.findViewById(R.id.tv_timeline_description);
            View connector = view.findViewById(R.id.view_timeline_connector);

            tvTime.setText(item.getTime());
            setTextIfNotEmpty(tvTitle, item.getTitle());
            setTextIfNotEmpty(tvDescription, item.getDescription());
            connector.setVisibility(i == timelineItems.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            timelineContainer.addView(view);
        }
    }

    private void renderReviews(List<ReviewDisplayItem> reviews) {
        reviewsContainer.removeAllViews();
        if (reviews == null || reviews.isEmpty()) {
            tvReviewsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvReviewsEmpty.setVisibility(View.GONE);
        for (ReviewDisplayItem review : reviews) {
            View view = inflater.inflate(R.layout.item_review, reviewsContainer, false);
            TextView tvReviewer = view.findViewById(R.id.tv_review_name);
            TextView tvRating = view.findViewById(R.id.tv_review_rating);
            TextView tvComment = view.findViewById(R.id.tv_review_comment);

            tvReviewer.setText(review.getReviewerName());
            tvRating.setText(getString(R.string.event_detail_rating_value, review.getRating()));
            tvComment.setText(review.getComment());
            reviewsContainer.addView(view);
        }
    }
    private void renderWeather(EventDetail detail) {
        if (cardWeather == null || tvWeatherSummary == null) {
            return;
        }
        if (!weatherPlanner.isOutdoorEvent(detail)) {
            cardWeather.setVisibility(View.GONE);
            tvWeatherUnavailable.setVisibility(View.VISIBLE);
            tvWeatherUnavailable.setText(R.string.event_detail_weather_indoor_hint);
            return;
        }
        tvWeatherUnavailable.setVisibility(View.VISIBLE);
        tvWeatherUnavailable.setText(R.string.event_detail_weather_loading);
        cardWeather.setVisibility(View.GONE);

        weatherPlanner.fetchForecast(detail, new OutdoorWeatherPlanner.ForecastCallback() {
            @Override
            public void onSuccess(WeatherForecast forecast) {
                renderWeatherCard(forecast);
            }

            @Override
            public void onError(String message) {
                cardWeather.setVisibility(View.GONE);
                tvWeatherUnavailable.setVisibility(View.VISIBLE);
                tvWeatherUnavailable.setText(!TextUtils.isEmpty(message)
                        ? message
                        : getString(R.string.event_detail_weather_missing));
            }
        });
    }

    private void renderWeatherCard(WeatherForecast forecast) {
        if (forecast == null) {
            cardWeather.setVisibility(View.GONE);
            tvWeatherUnavailable.setVisibility(View.VISIBLE);
            tvWeatherUnavailable.setText(R.string.event_detail_weather_missing);
            return;
        }
        cardWeather.setVisibility(View.VISIBLE);
        tvWeatherUnavailable.setVisibility(View.GONE);

        tvWeatherSummary.setText(forecast.getSummary());
        tvWeatherTime.setText(forecast.getTimeWindow());
        tvWeatherTemperature.setText(getString(R.string.event_detail_weather_temperature,
                forecast.getTemperatureC()));
        tvWeatherRain.setText(getString(R.string.event_detail_weather_rain, forecast.getRainChance()));
        tvWeatherRecommendation.setText(forecast.getRecommendation());
        imgWeatherIcon.setImageResource(mapWeatherIcon(forecast.getCondition()));
    }

    private int mapWeatherIcon(WeatherForecast.Condition condition) {
        switch (condition) {
            case SUNNY:
                return R.drawable.ic_weather_sunny;
            case CLOUDY:
                return R.drawable.ic_weather_partly_cloudy;
            case RAINY:
                return R.drawable.ic_weather_rain;
            case STORMY:
                return R.drawable.ic_weather_storm;
            default:
                return R.drawable.ic_weather_partly_cloudy;
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showErrorMessage(String message) {
        errorContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    private void setTextIfNotEmpty(TextView textView, String text) {
        if (textView == null || TextUtils.isEmpty(text)) {
            return;
        }
        textView.setText(text);
    }

    private String formatSchedule(@Nullable String startIso, @Nullable String endIso) {
        if (TextUtils.isEmpty(startIso) && TextUtils.isEmpty(endIso)) {
            return getString(R.string.event_detail_schedule_pending);
        }
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(startIso)) {
            builder.append(startIso.replace("T", " ").replace("Z", ""));
        }
        if (!TextUtils.isEmpty(endIso)) {
            builder.append(" - ").append(endIso.replace("T", " ").replace("Z", ""));
        }
        return builder.toString();
    }
}
