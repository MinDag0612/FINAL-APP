package com.FinalProject.feature_event_detail.data;

import com.FinalProject.feature_event_detail.model.EventDetail;
import com.FinalProject.feature_event_detail.model.ReviewDisplayItem;
import com.FinalProject.feature_event_detail.model.TicketTier;
import com.FinalProject.feature_event_detail.model.TimelineItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sinh dữ liệu giả phục vụ preview UI khi chưa có kết nối tới Firestore.
 */
public final class MockEventDetailFactory {

    private MockEventDetailFactory() {}

    public static EventDetail create() {
        List<TicketTier> tiers = Arrays.asList(
                new TicketTier("Ghế phổ thông", 690_000, 200, 148),
                new TicketTier("Ghế VIP", 1_290_000, 80, 64),
                new TicketTier("Ghế Premium", 1_890_000, 40, 32)
        );
        List<TimelineItem> timeline = Arrays.asList(
                new TimelineItem("17:30", "Đón khách", "Check-in & Welcome drink"),
                new TimelineItem("18:30", "Phiên 1", "4 câu chuyện từ diễn giả trẻ"),
                new TimelineItem("20:30", "After party", "Networking và ký tặng")
        );
        List<ReviewDisplayItem> reviews = Arrays.asList(
                new ReviewDisplayItem("Trâm Nguyễn", 5, "Tổ chức chỉn chu, phần networking VIP cực kỳ đáng giá."),
                new ReviewDisplayItem("Lê Hoàng", 4, "Nội dung truyền cảm hứng, mong có thêm hoạt động trải nghiệm."),
                new ReviewDisplayItem("Kiệt Trần", 5, "Lần đầu tham gia TEDx nhưng bị ấn tượng mạnh với dàn speaker.")
        );

        return new EventDetail(
                "mock_event_id",
                "TEDxYouth Saigon 2024",
                "8 câu chuyện truyền cảm hứng từ các nhà sáng tạo trẻ về giáo dục, nghệ thuật và công nghệ.",
                "Nhà hát Thành phố, Quận 1",
                "Innovation",
                "2024-12-15T18:30:00Z",
                "2024-12-15T22:00:00Z",
                "Nghệ sĩ Bích Phương, CTO VNG",
                null,
                Arrays.asList("Innovation", "Offline", "Youth"),
                tiers,
                timeline,
                reviews,
                4.8,
                reviews.size()
        );
    }

    public static EventDetail emptyState() {
        return new EventDetail(
                "mock_event_id",
                "Sự kiện đang cập nhật",
                "Thông tin chi tiết sự kiện sẽ hiển thị tại đây sau khi bạn chọn một vé bất kỳ.",
                "",
                "",
                null,
                null,
                null,
                null,
                Collections.<String>emptyList(),
                Collections.<TicketTier>emptyList(),
                Collections.<TimelineItem>emptyList(),
                Collections.<ReviewDisplayItem>emptyList(),
                0d,
                0
        );
    }
}
