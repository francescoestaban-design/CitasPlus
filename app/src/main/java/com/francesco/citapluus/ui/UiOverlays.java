package com.francesco.citapluus.ui;

import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class UiOverlays {
    private final View loading;
    private final View empty;
    private final TextView emptyMsg;
    private final MaterialButton emptyAction;

    public UiOverlays(View loadingRoot, View emptyRoot) {
        this.loading = loadingRoot;
        this.empty = emptyRoot;
        this.emptyMsg = emptyRoot.findViewById(
                emptyRoot.getResources().getIdentifier("emptyMessage","id", emptyRoot.getContext().getPackageName()));
        this.emptyAction = emptyRoot.findViewById(
                emptyRoot.getResources().getIdentifier("emptyAction","id", emptyRoot.getContext().getPackageName()));
    }

    public void showLoading() {
        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
    }

    public void hideLoading() {
        loading.setVisibility(View.GONE);
    }

    public void showEmpty(CharSequence msg, CharSequence actionText, View.OnClickListener action) {
        if (msg != null) emptyMsg.setText(msg);
        if (actionText != null && action != null) {
            emptyAction.setText(actionText);
            emptyAction.setOnClickListener(action);
            emptyAction.setVisibility(View.VISIBLE);
        } else {
            emptyAction.setVisibility(View.GONE);
        }
        empty.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }

    public void hideAll() {
        loading.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
    }
}
