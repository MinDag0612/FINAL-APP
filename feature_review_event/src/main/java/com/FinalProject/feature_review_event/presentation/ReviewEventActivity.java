package com.FinalProject.feature_review_event.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.FinalProject.feature_review_event.R;
import com.FinalProject.feature_review_event.data.ReviewEventRepository;
import com.FinalProject.feature_review_event.domain.LoadReviewEventInfoUseCase;
import com.FinalProject.feature_review_event.domain.SubmitReviewUseCase;
import com.FinalProject.feature_review_event.model.ReviewEventContent;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class ReviewEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_SCHEDULE = "extra_event_schedule";
    public static final String EXTRA_EVENT_LOCATION = "extra_event_location";

    private MaterialToolbar toolbar;
    private TextView tvEventName;
    private TextView tvEventSchedule;
    private TextView tvEventLocation;
    private TextView tvStatusMessage;
    private TextView tvRatingPlaceholder;
    private RatingBar ratingBar;
    private TextInputLayout inputComment;
    private MaterialButton btnSubmit;
    private CircularProgressIndicator progressIndicator;
    private ViewGroup contentContainer;

    private LoadReviewEventInfoUseCase loadReviewEventInfoUseCase;
    private SubmitReviewUseCase submitReviewUseCase;
    private String eventId;
    private boolean alreadySubmitted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_FeatureReviewEvent);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_event);

        ReviewEventRepository repository = new ReviewEventRepository();
        loadReviewEventInfoUseCase = new LoadReviewEventInfoUseCase(repository);
        submitReviewUseCase = new SubmitReviewUseCase(repository);

        bindViews();
        configureToolbar();
        applyFallback(getIntent());
        setupListeners();
        loadContent();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_review_event);
        tvEventName = findViewById(R.id.tv_event_name);
        tvEventSchedule = findViewById(R.id.tv_event_schedule);
        tvEventLocation = findViewById(R.id.tv_event_location);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        tvRatingPlaceholder = findViewById(R.id.tv_rating_placeholder);
        ratingBar = findViewById(R.id.rating_event_experience);
        inputComment = findViewById(R.id.input_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);
        progressIndicator = findViewById(R.id.progress_review_event);
        contentContainer = findViewById(R.id.layout_review_content);
    }

    private void configureToolbar() {
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void applyFallback(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        setTextIfNotEmpty(tvEventName, intent.getStringExtra(EXTRA_EVENT_TITLE));
        setTextIfNotEmpty(tvEventSchedule, intent.getStringExtra(EXTRA_EVENT_SCHEDULE));
        setTextIfNotEmpty(tvEventLocation, intent.getStringExtra(EXTRA_EVENT_LOCATION));
    }

    private void setupListeners() {
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> updateRatingText(rating));
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void loadContent() {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.review_event_missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        showLoading(true);
        loadReviewEventInfoUseCase.execute(eventId, new LoadReviewEventInfoUseCase.Callback() {
            @Override
            public void onSuccess(ReviewEventContent content) {
                showLoading(false);
                bindContent(content);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Toast.makeText(ReviewEventActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindContent(ReviewEventContent content) {
        tvEventName.setText(content.getEventName());
        tvEventSchedule.setText(content.getEventSchedule());
        tvEventLocation.setText(content.getEventLocation());
        updateReviewState(content.isAlreadyReviewed());
    }

    private void updateReviewState(boolean alreadyReviewed) {
        alreadySubmitted = alreadyReviewed;
        tvStatusMessage.setVisibility(alreadyReviewed ? View.VISIBLE : View.GONE);
        if (alreadyReviewed) {
            tvStatusMessage.setText(R.string.review_event_already_submitted);
        }
        ratingBar.setIsIndicator(alreadyReviewed);
        ratingBar.setEnabled(!alreadyReviewed);
        inputComment.setEnabled(!alreadyReviewed);
        btnSubmit.setEnabled(!alreadyReviewed);
    }

    private void submitReview() {
        if (alreadySubmitted) {
            return;
        }
        int rating = Math.round(ratingBar.getRating());
        TextInputEditText editText = (TextInputEditText) inputComment.getEditText();
        String comment = editText != null ? editText.getText().toString() : "";

        if (rating == 0) {
            inputComment.setError(null);
            Toast.makeText(this, R.string.review_event_error_invalid_rating, Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.trim().length() < 10) {
            inputComment.setError(getString(R.string.review_event_error_comment_short));
            return;
        }
        inputComment.setError(null);
        setSubmitting(true);
        submitReviewUseCase.execute(eventId, rating, comment, new SubmitReviewUseCase.Callback() {
            @Override
            public void onSuccess() {
                setSubmitting(false);
                Toast.makeText(ReviewEventActivity.this, R.string.review_event_success, Toast.LENGTH_LONG).show();
                updateReviewState(true);
            }

            @Override
            public void onError(String message) {
                setSubmitting(false);
                Toast.makeText(ReviewEventActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRatingText(float rating) {
        if (rating <= 0f) {
            tvRatingPlaceholder.setText(R.string.review_event_rating_placeholder);
        } else {
            tvRatingPlaceholder.setText(String.format(Locale.getDefault(), "Bạn chấm %.0f/5 sao", rating));
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        contentContainer.setAlpha(show ? 0.4f : 1f);
        contentContainer.setEnabled(!show);
        btnSubmit.setEnabled(!show && !alreadySubmitted);
    }

    private void setSubmitting(boolean submitting) {
        btnSubmit.setEnabled(!submitting);
        btnSubmit.setText(submitting ? getString(R.string.review_event_submitting) : getString(R.string.review_event_submit));
    }

    private void setTextIfNotEmpty(TextView textView, @Nullable String value) {
        if (textView == null || TextUtils.isEmpty(value)) {
            return;
        }
        textView.setText(value);
    }
}
