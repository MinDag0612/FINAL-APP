package com.FinalProject.feature_home.presentation;

import android.content.ClipData;
import android.content.ClipboardManager;// Addedimport android.content.ClipboardManager; // Added
import android.content.Context; // Added
import android.content.Intent;
import android.net.Uri; // Added
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.FinalProject.core.constName.StoreField;
import com.FinalProject.core.util.Event_API;
import com.FinalProject.feature_event_detail.presentation.EventDetailNavigator;
import com.FinalProject.feature_home.R;
import com.FinalProject.feature_home.data.EventRepository;
import com.FinalProject.feature_home.data.HomeRepository;
import com.FinalProject.feature_home.domain.GetHomeContentUseCase;
import com.FinalProject.feature_home.model.HomeContent;
import com.FinalProject.feature_home.model.HomeEvent;
import com.FinalProject.feature_home.model.HomeUser;
import com.FinalProject.feature_home.model.RecentTicketInfo;
import com.FinalProject.feature_home.presentation.adapter.HomeArtistAdapter;
import com.FinalProject.feature_home.presentation.adapter.HomeEventAdapter;
import com.FinalProject.feature_profile.presentation.ProfileNavigator;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// CHANGE 1: Implement both listeners
public class HomeActivity extends AppCompatActivity implements HomeEventAdapter.EventClickListener, HomeEventAdapter.EventShareListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvGreeting;
    private TextView tvSubtitle;
    private TextView tvFeaturedTitle;
    private TextView tvFeaturedDate;
    private TextView tvFeaturedLocation;
    private TextView tvFeaturedDescription;
    private TextView tvFeaturedPrice;
    private TextView tvRecentTicketTitle;
    private TextView tvRecentTicketSubtitle;
    private TextView tvEventsEmpty;
    private MaterialButton btnFeaturedAction;
    private MaterialButton btnRecentTicket;
    private ChipGroup chipGroup;
    private RecyclerView rvEvents;
    private RecyclerView rvArtists;
    private ShapeableImageView imgAvatar;
    private TextView tvAvatarInitial;

    private HomeEventAdapter eventAdapter;
    private HomeArtistAdapter artistAdapter;
    private final List<HomeEvent> allEvents = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private final SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault());

    private GetHomeContentUseCase getHomeContentUseCase;

    private TextInputEditText homeSearch;

    List<HomeEvent> filteredEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_FeatureHome);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        inputDateFormat.setLenient(false);
        getHomeContentUseCase = new GetHomeContentUseCase(new HomeRepository());

        initViews();
        setupRecyclerViews();
        setupListeners();
        loadHomeContent();
        setHomeSearch();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_home);
        tvGreeting = findViewById(R.id.tv_home_greeting);
        tvSubtitle = findViewById(R.id.tv_home_subtitle);
        tvFeaturedTitle = findViewById(R.id.tv_featured_title);
        tvFeaturedDate = findViewById(R.id.tv_featured_date);
        tvFeaturedLocation = findViewById(R.id.tv_featured_location);
        tvFeaturedDescription = findViewById(R.id.tv_featured_description);
        tvFeaturedPrice = findViewById(R.id.tv_featured_price);
        tvRecentTicketTitle = findViewById(R.id.tv_recent_ticket_title);
        tvRecentTicketSubtitle = findViewById(R.id.tv_recent_ticket_subtitle);
        tvEventsEmpty = findViewById(R.id.tv_empty_events);
        btnFeaturedAction = findViewById(R.id.btn_featured_action);
        btnRecentTicket = findViewById(R.id.btn_view_ticket);
        chipGroup = findViewById(R.id.chip_group_categories);
        rvEvents = findViewById(R.id.rv_events);
        rvArtists = findViewById(R.id.rv_artists);
        imgAvatar = findViewById(R.id.img_home_avatar);
        tvAvatarInitial = findViewById(R.id.tv_home_avatar_initial);
        homeSearch = findViewById(R.id.input_home_search);
    }

    private void setupRecyclerViews() {
        // CHANGE 2: Pass 'this' for both arguments in the constructor
        eventAdapter = new HomeEventAdapter(this, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
        rvEvents.setItemViewCacheSize(10);
        if (rvEvents.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) rvEvents.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        artistAdapter = new HomeArtistAdapter();
        rvArtists.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvArtists.setAdapter(artistAdapter);
        rvArtists.setItemViewCacheSize(10);
        if (rvArtists.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) rvArtists.getItemAnimator()).setSupportsChangeAnimations(false);
        }
    }

    // You already have onEventClick, so now you need to add onShareClick

    @Override
    public void onEventClick(HomeEvent event) {
        if (event == null || event.getId() == null) {
            Toast.makeText(this, "Cannot open this event's details", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = EventDetailNavigator.createIntent(
                this,
                event.getId(),
                event.getName(),
                event.getLocation(),
                formatDate(event.getStartTimeIso()),
                event.getStartingPrice()
        );
        startActivity(intent);
    }

    // CHANGE 3: Implement the onShareClick method from the interface
    // In HomeActivity.java

    // CHANGE 3: Implement the onShareClick method from the interface
    @Override
    public void onShareClick(HomeEvent event) {
        // Create the deep link
        String deepLink = "https://link-for-app.onrender.com/event/" + event.getId();

        // Get clipboard service
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Create ClipData
        ClipData clip = ClipData.newPlainText("EventLink", deepLink);

        // Set the data to the clipboard
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            // Notify the user
            Toast.makeText(this, "Event link copied to clipboard!", Toast.LENGTH_SHORT).show();
        } else {
            // Handle the case where clipboard service is not available
            Toast.makeText(this, "Could not access clipboard service.", Toast.LENGTH_SHORT).show();
        }
    }


    // ... The rest of your HomeActivity.java code remains the same ...

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadHomeContent);

        btnFeaturedAction.setOnClickListener(v -> {
            if (!allEvents.isEmpty()) {
                openEventDetail(allEvents.get(0));
            } else {
                Toast.makeText(this, R.string.home_button_book_now, Toast.LENGTH_SHORT).show();
            }
        });

        btnRecentTicket.setOnClickListener(v ->
                Toast.makeText(this, R.string.home_action_view_ticket, Toast.LENGTH_SHORT).show());

        View.OnClickListener profileClickListener = v -> openProfileScreen();
        imgAvatar.setOnClickListener(profileClickListener);
        tvAvatarInitial.setOnClickListener(profileClickListener);
    }

    private void loadHomeContent() {
        swipeRefreshLayout.setRefreshing(true);
        getHomeContentUseCase.execute(new GetHomeContentUseCase.Callback() {
            @Override
            public void onSuccess(HomeContent content) {
                swipeRefreshLayout.setRefreshing(false);
                bindHomeContent(content);
            }

            @Override
            public void onError(String message) {
                swipeRefreshLayout.setRefreshing(false);
                showError(message);
            }
        });
    }

    private void bindHomeContent(HomeContent content) {
        bindUser(content.getUser());
        bindEvents(content.getEvents());
        artistAdapter.submitList(content.getArtists());
        bindRecentTicket(content.getRecentTicketInfo());
    }

    private void bindUser(HomeUser user) {
        if (user == null) {
            tvGreeting.setText(getString(R.string.home_greeting));
            return;
        }
        String greeting = user.getFirstName().isEmpty()
                ? getString(R.string.home_greeting)
                : getString(R.string.home_greeting_named, user.getFirstName());
        tvGreeting.setText(greeting);
        setAvatarInitial(user.getFirstName());
    }

    private void setAvatarInitial(String name) {
        String initial = (name == null || name.isEmpty())
                ? "T"
                : name.substring(0, 1).toUpperCase(Locale.ROOT);
        tvAvatarInitial.setText(initial);
        imgAvatar.setContentDescription(initial);
    }

    private void bindEvents(List<HomeEvent> events) {
        allEvents.clear();
        if (events != null) {
            allEvents.addAll(events);
        }

        eventAdapter.submitList(allEvents);
        tvEventsEmpty.setVisibility(allEvents.isEmpty() ? View.VISIBLE : View.GONE);
        tvSubtitle.setText(allEvents.isEmpty()
                ? getString(R.string.home_no_event)
                : getString(R.string.home_subtitle_generic));

        if (!allEvents.isEmpty()) {
            bindFeaturedEvent(allEvents.get(0));
        }

        renderCategoryChips(allEvents);
    }

    private void bindFeaturedEvent(@NonNull HomeEvent event) {
        tvFeaturedTitle.setText(event.getName());
        tvFeaturedDate.setText(formatDate(event.getStartTimeIso()));
        tvFeaturedLocation.setText(event.getLocation());
        tvFeaturedDescription.setText(TextUtils.isEmpty(event.getDescription())
                ? getString(R.string.home_events_for_you)
                : event.getDescription());
        tvFeaturedPrice.setText(event.getStartingPrice() > 0
                ? currencyFormat.format(event.getStartingPrice())
                : getString(R.string.home_price_pending));

        btnFeaturedAction.setOnClickListener(v -> Toast.makeText(
                this,
                event.getName(),
                Toast.LENGTH_SHORT
        ).show());
    }

    private void renderCategoryChips(List<HomeEvent> events) {
        chipGroup.setOnCheckedStateChangeListener(null);
        chipGroup.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        Chip allChip = (Chip) inflater.inflate(R.layout.item_filter_chip, chipGroup, false);
        allChip.setText(R.string.home_events_for_you);
        allChip.setChecked(true);
        chipGroup.addView(allChip);

        Set<String> categories = extractCategories(events);
        for (String category : categories) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_filter_chip, chipGroup, false);
            chip.setText(category);
            chipGroup.addView(chip);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                group.check(allChip.getId());
                return;
            }
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                filterEventsByCategory(chip.getText().toString());
            }
        });
    }

    private Set<String> extractCategories(List<HomeEvent> events) {
        if (events == null || events.isEmpty()) {
            return new HashSet<>();
        }
        return events.stream()
                .map(HomeEvent::getEventType)
                .filter(type -> type != null && !type.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void filterEventsByCategory(String category) {
        if (category.equals(getString(R.string.home_events_for_you))) {
            eventAdapter.submitList(new ArrayList<>(allEvents));
            tvSubtitle.setText(getString(R.string.home_subtitle_generic));
            return;
        }
        List<HomeEvent> filtered = new ArrayList<>();
        for (HomeEvent event : allEvents) {
            if (event.getEventType() != null && event.getEventType().equalsIgnoreCase(category)) {
                filtered.add(event);
            }
        }
        eventAdapter.submitList(filtered);
        tvSubtitle.setText(getString(R.string.home_subtitle_filtered, filtered.size()));
    }

    private void bindRecentTicket(RecentTicketInfo recentTicketInfo) {
        if (recentTicketInfo == null) {
            tvRecentTicketTitle.setText(R.string.home_no_event);
            tvRecentTicketSubtitle.setText("");
            btnRecentTicket.setEnabled(false);
            return;
        }
    }

    private void showError(String message) {
        Snackbar.make(swipeRefreshLayout, "Error: " + message, Snackbar.LENGTH_LONG).show();
    }

    private void openProfileScreen() {
        startActivity(ProfileNavigator.createIntent(this));
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        try {
            return outputDateFormat.format(inputDateFormat.parse(raw));
        } catch (ParseException ignored) {
            return raw;
        }
    }

    private void openEventDetail(HomeEvent event) {
        if (event == null || event.getId() == null) {
            Toast.makeText(this, "Cannot open this event's details", Toast.LENGTH_SHORT).show();
            return;
        }
        android.util.Log.d("HomeActivity", "openEventDetail - eventId: " + event.getId() + ", name: " + event.getName());
        Intent intent = EventDetailNavigator.createIntent(
                this,
                event.getId(),
                event.getName(),
                event.getLocation(),
                formatDate(event.getStartTimeIso()),
                event.getStartingPrice()
        );
        startActivity(intent);
    }

    private void setHomeSearch() {
        // Implement search logic
        HomeSearch search = new HomeSearch(homeSearch, allEvents, eventAdapter);
        search.setupSearchListener();
    }
}
