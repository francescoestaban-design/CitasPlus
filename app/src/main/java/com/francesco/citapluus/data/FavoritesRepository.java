package com.francesco.citapluus.data;

import android.content.Context;

import com.francesco.citapluus.FavoritePlace;
import com.francesco.citapluus.net.core.ApiService;
import com.francesco.citapluus.net.core.RetrofitProvider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesRepository {
    private final ApiService api;

    public FavoritesRepository(Context ctx) {
        api = RetrofitProvider.get(ctx).create(ApiService.class);
    }

    public void fetch(RepoCallback<List<FavoritePlace>> cb) {
        api.getFavorites().enqueue(new Callback<List<FavoritePlace>>() {
            @Override public void onResponse(Call<List<FavoritePlace>> call, Response<List<FavoritePlace>> resp) {
                if (resp.isSuccessful() && resp.body()!=null) cb.onSuccess(resp.body());
                else cb.onError(new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<List<FavoritePlace>> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    public void add(FavoritePlace fav, RepoCallback<FavoritePlace> cb) {
        api.addFavorite(fav).enqueue(new Callback<FavoritePlace>() {
            @Override public void onResponse(Call<FavoritePlace> call, Response<FavoritePlace> resp) {
                if (resp.isSuccessful() && resp.body()!=null) cb.onSuccess(resp.body());
                else cb.onError(new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<FavoritePlace> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    public void remove(String id, RepoCallback<Void> cb) {
        api.deleteFavorite(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                if (resp.isSuccessful()) cb.onSuccess(null);
                else cb.onError(new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    public interface RepoCallback<T> {
        void onSuccess(T data);
        void onError(Throwable error);
    }
}
