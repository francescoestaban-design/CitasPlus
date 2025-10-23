package com.francesco.citapluus.net.core;

import com.francesco.citapluus.FavoritePlace;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // Salud
    @GET("ping")
    Call<PingResp> ping();

    class PingResp { public String msg; }

    // --- Favoritos ---
    @GET("favorites")
    Call<List<FavoritePlace>> getFavorites();

    @POST("favorites")
    Call<FavoritePlace> addFavorite(@Body FavoritePlace fav);

    @DELETE("favorites/{id}")
    Call<Void> deleteFavorite(@Path("id") String id);
}
