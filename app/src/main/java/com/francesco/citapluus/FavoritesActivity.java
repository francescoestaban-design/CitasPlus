package com.francesco.citapluus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private FavoritesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        MaterialToolbar toolbar = findViewById(R.id.topBarFav);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerFav);
        emptyView = findViewById(R.id.emptyFav);

        SessionManager sm = new SessionManager(this);
        List<FavoritePlace> data = new ArrayList<>(sm.getFavoritos());
        Collections.sort(data, Comparator.comparing(f -> f.nombre.toLowerCase()));

        adapter = new FavoritesAdapter(data, new FavoritesAdapter.Callback() {
            @Override
            public void onGo(FavoritePlace f) {
                Intent i = new Intent();
                i.putExtra("lat", f.lat);
                i.putExtra("lng", f.lng);
                i.putExtra("nombre", f.nombre);
                i.putExtra("direccion", f.direccion);
                setResult(RESULT_OK, i);
                finish();
            }

            @Override
            public void onDelete(FavoritePlace f) {
                sm.removeFavoritoById(f.id);
                data.remove(f);
                adapter.notifyDataSetChanged();
                updateEmpty(data.isEmpty());
                Toast.makeText(FavoritesActivity.this, "Eliminado", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        updateEmpty(data.isEmpty());
    }

    private void updateEmpty(boolean empty) {
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
