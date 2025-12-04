package com.FinalProject.feature_home.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.core.model.NewsFeed;
import com.FinalProject.feature_home.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsFeed> items = new ArrayList<>();
    private OnNewsClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnNewsClickListener {
        void onNewsClick(NewsFeed news);
        void onLikeClick(NewsFeed news, int position);
    }

    public NewsAdapter(OnNewsClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<NewsFeed> newsList) {
        items.clear();
        if (newsList != null) {
            items.addAll(newsList);
        }
        notifyDataSetChanged();
    }

    public void updateLikeCount(int position, int newCount) {
        if (position >= 0 && position < items.size()) {
            items.get(position).setLike_count(newCount);
            notifyItemChanged(position);
        }
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvTitle;
        private final TextView tvContent;
        private final TextView tvDate;
        private final TextView tvViews;
        private final TextView tvLikes;
        private final ImageView ivLike;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvTitle = itemView.findViewById(R.id.tv_news_title);
            tvContent = itemView.findViewById(R.id.tv_news_content);
            tvDate = itemView.findViewById(R.id.tv_news_date);
            tvViews = itemView.findViewById(R.id.tv_news_views);
            tvLikes = itemView.findViewById(R.id.tv_news_likes);
            ivLike = itemView.findViewById(R.id.iv_news_like);
        }

        void bind(NewsFeed news, int position) {
            tvTitle.setText(news.getTitle());
            tvContent.setText(news.getContent());
            
            // Format date
            try {
                Date date = new Date(news.getPublished_at());
                tvDate.setText(dateFormat.format(date));
            } catch (Exception e) {
                tvDate.setText("");
            }

            tvViews.setText(String.valueOf(news.getView_count()));
            tvLikes.setText(String.valueOf(news.getLike_count()));

            // Click to view details
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNewsClick(news);
                }
            });

            // Click to like
            ivLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(news, position);
                }
            });
        }
    }
}
