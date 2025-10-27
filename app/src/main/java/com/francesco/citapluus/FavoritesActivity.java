package com.francesco.citapluus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francesco.citapluus.data.FavoritesRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
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

        recyclerView = findViewById(R.id.recyclerFav);
        emptyView = findViewById(R.id.emptyFav);

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
                com.francesco.citapluus.data.FavoritesRepository repo =
                        new com.francesco.citapluus.data.FavoritesRepository(FavoritesActivity.this);

                repo.remove(f.id, new com.francesco.citapluus.data.FavoritesRepository.RepoCallback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Toast.makeText(FavoritesActivity.this, getString(R.string.msg_fav_eliminado), Toast.LENGTH_SHORT).show();
                        // Recargar lista desde servidor para mantener todo alineado
                        repo.list(new com.francesco.citapluus.data.FavoritesRepository.RepoCallback<List<FavoritePlace>>() {
                            @Override
                            public void onSuccess(List<FavoritePlace> fresh) {
                                adapter.setData(fresh);
                                data.clear();
                                data.addAll(fresh);
                                updateEmpty(data.isEmpty());
                            }

                            @Override
                            public void onError(Throwable error) {
                                // fallback local si algo falló
                                SessionManager sm = new SessionManager(FavoritesActivity.this);
                                List<FavoritePlace> fallback = new ArrayList<>(sm.getFavoritos());
                                adapter.setData(fallback);
                                data.clear();
                                data.addAll(fallback);
                                updateEmpty(data.isEmpty());
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        Toast.makeText(FavoritesActivity.this, "No se pudo eliminar (red).", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        // Swipe para borrar
        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            FavoritePlace swiped;

            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                swiped = adapter.getItem(pos);
                // optimista: quitamos de UI ya
                adapter.remove(swiped);
                updateEmpty(adapter.getItemCount() == 0);

                borrarEnServidor(swiped, /*onFail=*/() -> {
                    // Reponemos si falló
                    data.add(pos, swiped);
                    adapter.setData(new ArrayList<>(data));
                    updateEmpty(false);
                });
            }
        });
        ith.attachToRecyclerView(recyclerView);

        reloadFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadFromServer();
    }

    private void reloadFromServer() {
        repo.list(new FavoritesRepository.RepoCallback<List<FavoritePlace>>() {
            @Override
            public void onSuccess(List<FavoritePlace> fresh) {
                // ordenar por nombre (opcional)
                Collections.sort(fresh, (a, b) -> {
                    String na = a != null && a.nombre != null ? a.nombre : "";
                    String nb = b != null && b.nombre != null ? b.nombre : "";
                    return na.compareToIgnoreCase(nb);
                });
                data.clear();
                data.addAll(fresh);
                adapter.setData(new ArrayList<>(data));
                updateEmpty(data.isEmpty());
            }

            @Override
            public void onError(Throwable t) {
                // Dejar silencio o mostrar un aviso suave
                Snackbar.make(recyclerView, "No se pudo cargar favoritos.", Snackbar.LENGTH_SHORT).show();
                updateEmpty(data.isEmpty());
            }
        });
    }

    private void borrarEnServidor(FavoritePlace f) {
        borrarEnServidor(f, null);
    }

    private void borrarEnServidor(FavoritePlace f, @Nullable Runnable onFail) {
        if (f == null) return;
        repo.remove(f.id, new FavoritesRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                // Asegurarnos de reflejar el borrado en la lista local (por si vino de icono)
                int idx = -1;
                for (int i = 0; i < data.size(); i++)
                    if (f.id.equals(data.get(i).id)) {
                        idx = i;
                        break;
                    }
                if (idx >= 0) {
                    data.remove(idx);
                    adapter.setData(new ArrayList<>(data));
                    updateEmpty(data.isEmpty());
                }
                Toast.makeText(FavoritesActivity.this, getString(R.string.msg_fav_eliminado), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable error) {
                if (onFail != null) onFail.run();
                Snackbar.make(recyclerView, "Error eliminando. Revisa conexión.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void updateEmpty(boolean empty) {
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
