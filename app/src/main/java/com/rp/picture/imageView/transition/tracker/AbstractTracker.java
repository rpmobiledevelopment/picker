package com.rp.picture.imageView.transition.tracker;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface AbstractTracker<ID> {

    int NO_POSITION = -1;

    int getPositionById(@NonNull ID id);

    @Nullable
    View getViewById(@NonNull ID id);

}
