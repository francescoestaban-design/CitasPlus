package com.francesco.citapluus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SuggestAdapter extends RecyclerView.Adapter<SuggestAdapter.VH> {
    public interface OnSuggClick {
        void onClick(String texto);
    }

    private final List<String> data = new ArrayList<>();
    private final OnSuggClick listener;

    public SuggestAdapter(OnSuggClick l) { this.listener = l; }

    public void setData(List<String> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() { setData(null); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vtype) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_sugerencia, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        String s = data.get(i);
        h.txt.setText(s);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txt;
        VH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.textSugerencia);
        }
    }
}
