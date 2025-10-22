package com.francesco.citapluus.data;

import android.content.Context;

import com.francesco.citapluus.FavoritePlace;
import com.francesco.citapluus.SessionManager;
import com.francesco.citapluus.net.api.FavoritesApi;
import com.francesco.citapluus.net.core.RetrofitProvider;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesRepository {

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(Throwable t);
    }

    private final FavoritesApi api;
    private final SessionManager local;

    public FavoritesRepository(Context ctx) {
        // ✅ pasa el Context a RetrofitProvider.get(ctx)
        this.api = RetrofitProvider.get(ctx).create(FavoritesApi.class);
        this.local = new SessionManager(ctx);
    }

    /** Lista favoritos: intenta red; si falla, usa local. */
    public void list(RepoCallback<List<FavoritePlace>> cb) {
        api.getFavorites().enqueue(new Callback<List<FavoritePlace>>() {
            @Override public void onResponse(Call<List<FavoritePlace>> call, Response<List<FavoritePlace>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    cb.onSuccess(resp.body());
                } else {
                    cb.onSuccess(new ArrayList<>(local.getFavoritos())); // fallback local
                }
            }
            @Override public void onFailure(Call<List<FavoritePlace>> call, Throwable t) {
                cb.onSuccess(new ArrayList<>(local.getFavoritos()));   // fallback local
            }
        });
    }

    /** Añadir: intenta red; si falla, guarda local. */
    public void add(FavoritePlace f, RepoCallback<Void> cb) {
        api.addFavorite(f).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                if (!resp.isSuccessful()) local.addFavorito(f);
                cb.onSuccess(null);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                local.addFavorito(f);
                cb.onSuccess(null);
            }
        });
    }

    /** Eliminar: intenta red; si falla, borra local. */
    public void delete(FavoritePlace f, RepoCallback<Void> cb) {
        api.deleteFavorite(f.id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                if (!resp.isSuccessful()) local.removeFavoritoById(f.id);
                cb.onSuccess(null);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                local.removeFavoritoById(f.id);
                cb.onSuccess(null);
            }
        });
    }
}
