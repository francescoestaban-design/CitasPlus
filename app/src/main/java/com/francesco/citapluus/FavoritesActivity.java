package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francesco.citapluus.data.FavoritesRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private FavoritesAdapter adapter;
    private FavoritesRepository repo;

    private final List<FavoritePlace> data = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        repo = new FavoritesRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.topBarFav);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.inflateMenu(R.menu.menu_favorites);

        toolbar.setOnMenuItemClickListener(item -> {
            final int id = item.getItemId();
            SessionManager sm = new SessionManager(this);
            List<FavoritePlace> list = new ArrayList<>(sm.getFavoritos());

            if (id == R.id.action_sort_type) {
                Collections.sort(list, (a, b) -> {
                    String ta = a.tipo == null ? "" : a.tipo;
                    String tb = b.tipo == null ? "" : b.tipo;
                    int cmp = ta.compareToIgnoreCase(tb);
                    if (cmp != 0) return cmp;
                    String na = a.nombre == null ? "" : a.nombre;
                    String nb = b.nombre == null ? "" : b.nombre;
                    return na.compareToIgnoreCase(nb);
                });
                adapter.setData(list);
                return true;

            } else if (id == R.id.action_sort_distance) {
                FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(this);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    fused.getLastLocation().addOnSuccessListener(loc -> {
                        if (loc == null) return;
                        final double lat0 = loc.getLatitude();
                        final double lng0 = loc.getLongitude();
                        list.sort((a, b) -> {
                            float[] d1 = new float[1];
                            float[] d2 = new float[1];
                            android.location.Location.distanceBetween(lat0, lng0, a.lat, a.lng, d1);
                            android.location.Location.distanceBetween(lat0, lng0, b.lat, b.lng, d2);
                            return Float.compare(d1[0], d2[0]);
                        });
                        adapter.setData(list);
                    });
                } else {
                    Toast.makeText(this, "Otorga permiso de ubicación para ordenar por distancia.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            return false;
        });

        recyclerView = findViewById(R.id.recyclerFav);
        emptyView = findViewById(R.id.emptyFav);

        adapter = new FavoritesAdapter(data, new FavoritesAdapter.Callback() {
            @Override public void onGo(FavoritePlace f) {
                Intent i = new Intent();
                i.putExtra("lat", f.lat);
                i.putExtra("lng", f.lng);
                i.putExtra("nombre", f.nombre);
                i.putExtra("direccion", f.direccion);
                setResult(RESULT_OK, i);
                finish();
            }

            @Override public void onDelete(FavoritePlace f) {
                SessionManager sm = new SessionManager(FavoritesActivity.this);
                sm.removeFavoritoById(f.id);
                data.remove(f);
                adapter.notifyDataSetChanged();
                updateEmpty(data.isEmpty());
                Toast.makeText(FavoritesActivity.this, getString(R.string.msg_fav_eliminado), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            FavoritePlace lastDeleted;

            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                lastDeleted = adapter.getItem(pos);
                SessionManager sm = new SessionManager(FavoritesActivity.this);
                sm.removeFavoritoById(lastDeleted.id);
                adapter.remove(lastDeleted);
                updateEmpty(adapter.getItemCount() == 0);

                Snackbar.make(recyclerView, getString(R.string.msg_fav_eliminado), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.snackbar_undo), v -> {
                            sm.addFavorito(lastDeleted);
                            reloadData();
                        })
                        .show();
            }
        });
        ith.attachToRecyclerView(recyclerView);

        reloadData();
    }

    @Override protected void onResume() {
        super.onResume();
        reloadData();
    }

    private void reloadData() {
        repo.fetch(new FavoritesRepository.RepoCallback<List<FavoritePlace>>() {
            @Override public void onSuccess(List<FavoritePlace> fresh) {
                java.util.Collections.sort(fresh, (a, b) -> {
                    String na = a != null && a.nombre != null ? a.nombre : "";
                    String nb = b != null && b.nombre != null ? b.nombre : "";
                    return na.compareToIgnoreCase(nb);
                });
                adapter.setData(fresh);
                data.clear();
                data.addAll(fresh);
                updateEmpty(data.isEmpty());
            }
            @Override public void onError(Throwable t) {
                // Si quieres, avisa al usuario o registra el error
                Toast.makeText(FavoritesActivity.this, "Error cargando favoritos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmpty(boolean empty) {
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
