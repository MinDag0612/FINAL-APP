package com.FinalProject.feature_home.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.FinalProject.feature_home.R;
import com.FinalProject.feature_home.model.HomeArtist;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeArtistAdapter extends RecyclerView.Adapter<HomeArtistAdapter.ArtistViewHolder> {

    private final List<HomeArtist> items = new ArrayList<>();

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<HomeArtist> artists) {
        items.clear();
        if (artists != null) {
            items.addAll(artists);
        }
        notifyDataSetChanged();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial;
        private final TextView tvName;
        private final TextView tvSubtitle;
        private final ShapeableImageView avatar;

        ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tv_artist_initial);
            tvName = itemView.findViewById(R.id.tv_artist_name);
            tvSubtitle = itemView.findViewById(R.id.tv_artist_subtitle);
            avatar = itemView.findViewById(R.id.img_artist_avatar);
        }

        void bind(HomeArtist artist) {
            tvName.setText(artist.getName());
            tvSubtitle.setText(itemView.getContext().getString(
                    R.string.home_artist_event_count,
                    artist.getEventCount()
            ));

            String initial = artist.getName().isEmpty()
                    ? "?"
                    : artist.getName().substring(0, 1).toUpperCase(Locale.ROOT);
            tvInitial.setText(initial);

            avatar.setContentDescription(artist.getName());
        }
    }
}
