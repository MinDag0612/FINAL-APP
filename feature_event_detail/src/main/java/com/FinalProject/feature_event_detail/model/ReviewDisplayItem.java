package com.FinalProject.feature_event_detail.model;

/**
 * Đối tượng hiển thị review trên UI, bao gồm thông tin người đánh giá.
 */
public class ReviewDisplayItem {
    private final String reviewerName;
    private final int rating;
    private final String comment;

    public ReviewDisplayItem(String reviewerName, int rating, String comment) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.comment = comment;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
