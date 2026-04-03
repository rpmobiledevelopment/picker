package com.rp.picture.imageView.transition.tracker;

import androidx.annotation.Nullable;

public interface IntoTracker<ID> extends AbstractTracker<ID> {

    @Nullable
    ID getIdByPosition(int position);

}
