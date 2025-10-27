package com.francesco.citapluus.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.francesco.citapluus.FavoritePlace;
import com.francesco.citapluus.SessionManager;
import com.francesco.citapluus.net.core.ApiService;
import com.francesco.citapluus.net.core.RetrofitProvider;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repositorio que llama a la API mock/real y sincroniza con SessionManager.
 * Loggea en tiempo real cada GET/POST/DELETE con tag "NetworkCfg".
 */
public class FavoritesRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(Throwable error);
    }

    private final Context ctx;
    private final ApiService api;
    private final SessionManager sm;

    public FavoritesRepository(@NonNull Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.api = RetrofitProvider.get(this.ctx).create(ApiService.class);
        this.sm  = new SessionManager(this.ctx);
    }

    /** GET /favorites */
    public void list(RepoCallback<List<FavoritePlace>> cb) {
        Log.i("NetworkCfg", "GET /favorites -> starting");
        api.listFavorites().enqueue(new Callback<List<FavoritePlace>>() {
            @Override public void onResponse(Call<List<FavoritePlace>> call, Response<List<FavoritePlace>> resp) {
                if (!resp.isSuccessful()) {
                    String msg = "GET /favorites HTTP " + resp.code();
                    Log.e("NetworkCfg", msg);
                    if (cb != null) cb.onError(new RuntimeException(msg));
                    return;
                }
                List<FavoritePlace> body = resp.body();
                List<FavoritePlace> safe = (body != null) ? body : new ArrayList<>();
                Log.i("NetworkCfg", "GET /favorites -> size=" + safe.size());

                // Sincroniza a prefs para lectura offline
                smSyncReplace(safe);

                if (cb != null) cb.onSuccess(safe);
            }
            @Override public void onFailure(Call<List<FavoritePlace>> call, Throwable t) {
                Log.e("NetworkCfg", "GET /favorites error", t);
                if (cb != null) cb.onError(t);
            }
        });
    }

    /** Alias por compatibilidad con código previo */
    public void fetch(RepoCallback<List<FavoritePlace>> cb) { list(cb); }

    /** POST /favorites */
    public void add(FavoritePlace nuevo, RepoCallback<FavoritePlace> cb) {
        if (nuevo == null) {
            if (cb != null) cb.onError(new IllegalArgumentException("nuevo == null"));
            return;
        }
        Log.i("NetworkCfg", "POST /favorites -> " + nuevo.id);
        api.addFavorite(nuevo).enqueue(new Callback<FavoritePlace>() {
            @Override public void onResponse(Call<FavoritePlace> call, Response<FavoritePlace> resp) {
                if (!resp.isSuccessful()) {
                    String msg = "POST /favorites HTTP " + resp.code();
                    Log.e("NetworkCfg", msg);
                    if (cb != null) cb.onError(new RuntimeException(msg));
                    return;
                }
                FavoritePlace saved = resp.body() != null ? resp.body() : nuevo;
                Log.i("NetworkCfg", "POST /favorites OK id=" + saved.id);

                // Actualiza prefs
                sm.addFavorito(saved);

                if (cb != null) cb.onSuccess(saved);
            }
            @Override public void onFailure(Call<FavoritePlace> call, Throwable t) {
                Log.e("NetworkCfg", "POST /favorites error", t);
                if (cb != null) cb.onError(t);
            }
        });
    }

    /** DELETE /favorites/{id} */
    public void remove(String id, RepoCallback<Void> cb) {
        if (id == null || id.isEmpty()) {
            if (cb != null) cb.onError(new IllegalArgumentException("id vacío"));
            return;
        }
        Log.i("NetworkCfg", "DELETE /favorites/" + id + " -> starting");
        api.deleteFavorite(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                if (!resp.isSuccessful()) {
                    String msg = "DELETE /favorites/" + id + " HTTP " + resp.code();
                    Log.e("NetworkCfg", msg);
                    if (cb != null) cb.onError(new RuntimeException(msg));
                    return;
                }
                Log.i("NetworkCfg", "DELETE /favorites/" + id + " OK");

                // Actualiza prefs
                sm.removeFavoritoById(id);

                if (cb != null) cb.onSuccess(null);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Log.e("NetworkCfg", "DELETE /favorites/" + id + " error", t);
                if (cb != null) cb.onError(t);
            }
        });
    }

    // ---------- Helpers ----------
    private void smSyncReplace(List<FavoritePlace> fresh) {
        List<FavoritePlace> old = sm.getFavoritos();
        for (FavoritePlace f : new ArrayList<>(old)) sm.removeFavoritoById(f.id);
        for (FavoritePlace f : fresh) sm.addFavorito(f);
    }
}
