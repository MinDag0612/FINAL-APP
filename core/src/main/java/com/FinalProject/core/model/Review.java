package com.FinalProject.core.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Review {     // review_id (Firestore document id)
    private String uid;       // user id của người đánh giá
    private int rate;      // điểm đánh giá
    private String comment;   // bình luận

    // Constructor rỗng bắt buộc cho Firestore
    public Review() {}

    public Review(String uid, int rate, String comment) {
        this.uid = uid;
        this.rate = rate;
        this.comment = comment;
    }


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("rate", rate);
        map.put("comment", comment);
        return map;
    }
}
