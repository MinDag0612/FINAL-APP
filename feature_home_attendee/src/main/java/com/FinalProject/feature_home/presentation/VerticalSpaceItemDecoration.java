package com.FinalProject.feature_home.presentation;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple decoration that adds vertical spacing between RecyclerView items.
 */
public class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int verticalSpace;

    public VerticalSpaceItemDecoration(int verticalSpace) {
        this.verticalSpace = verticalSpace;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        outRect.top = verticalSpace;
        if (position == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = verticalSpace;
        }
    }
}
