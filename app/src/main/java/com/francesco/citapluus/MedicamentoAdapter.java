package com.francesco.citapluus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.VH> {

    public interface OnDrugClickListener {
        void onDrugClick(DrugInfo info);
    }

    private final List<DrugInfo> data;
    private final OnDrugClickListener listener;

    public MedicamentoAdapter(List<DrugInfo> data, OnDrugClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView title, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            title = itemView.findViewById(R.id.textNombre);
            subtitle = itemView.findViewById(R.id.textResumen);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicamento, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DrugInfo d = data.get(position);
        h.title.setText(d.getNombre());
        h.subtitle.setText(d.getParaQueSirve());
        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onDrugClick(d);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }
}
