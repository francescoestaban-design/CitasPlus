package com.francesco.citapluus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.VH> {

    public interface Callback {
        void onGo(FavoritePlace f);
        void onDelete(FavoritePlace f);
    }

    private final List<FavoritePlace> data = new ArrayList<>();
    private final Callback callback;

    public FavoritesAdapter(@NonNull List<FavoritePlace> initial, @NonNull Callback callback) {
        if (initial != null) data.addAll(initial);
        this.callback = callback;
        setHasStableIds(true);
    }

    /** Reemplaza todo el dataset (útil si vuelves de FavoritesActivity con cambios). */
    public void setData(@NonNull List<FavoritePlace> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    /** Elimina 1 elemento por instancia (opcionalmente desde onDelete). */
    public void remove(@NonNull FavoritePlace f) {
        int idx = data.indexOf(f);
        if (idx >= 0) {
            data.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgIcono;
        TextView title, subtitle;
        ImageButton btnGo, btnDelete;

        VH(@NonNull View v) {
            super(v);
            imgIcono = v.findViewById(R.id.imgIcono);     // <-- debe existir en item_favorite.xml
            title    = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
            btnGo    = v.findViewById(R.id.btnGo);
            btnDelete= v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final FavoritePlace f = data.get(position);

        h.title.setText(f.nombre != null ? f.nombre : "");
        h.subtitle.setText(f.direccion != null ? f.direccion : "");

        // Icono por tipo (usa tus PNG: hospital.png / cruz_verde.png / favoritos.png)
        if (h.imgIcono != null) {
            if ("HOSPITAL".equalsIgnoreCase(f.tipo)) {
                h.imgIcono.setImageResource(R.drawable.cruz_verde);
                h.imgIcono.setContentDescription("Hospital");
            } else if ("FARMACIA".equalsIgnoreCase(f.tipo)) {
                h.imgIcono.setImageResource(R.drawable.cruz_verde);
                h.imgIcono.setContentDescription("Farmacia");
            } else {
                h.imgIcono.setImageResource(R.drawable.favoritos);
                h.imgIcono.setContentDescription("Favorito");
            }
        }

        h.btnGo.setOnClickListener(v -> {
            if (callback != null) callback.onGo(f);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (callback != null) callback.onDelete(f);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /** IDs estables para mejores animaciones del RecyclerView. */
    @Override
    public long getItemId(int position) {
        FavoritePlace f = data.get(position);
        // Si el id de Place existe lo usamos; si no, derivamos de lat/lng/nombre
        if (f.id != null) return f.id.hashCode();
        long mix = Double.doubleToRawLongBits(f.lat) ^ Double.doubleToRawLongBits(f.lng);
        return (f.nombre != null ? f.nombre.hashCode() : 0) ^ mix;
    }

    public FavoritePlace getItem(int position) {
        return data.get(position);
    }
}
