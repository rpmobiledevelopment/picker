package com.rp.picture.imageView.transition.tracker;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class SimpleTracker implements FromTracker<Integer>, IntoTracker<Integer> {

    @Override
    public Integer getIdByPosition(int position) {
        return position;
    }

    @Override
    public int getPositionById(@NonNull Integer id) {
        return id;
    }

    @Override
    public View getViewById(@NonNull Integer id) {
        return getViewAt(id);
    }

    @Nullable
    protected abstract View getViewAt(int position);

}
