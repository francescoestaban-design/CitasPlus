package com.francesco.citapluus.net.api;

import com.francesco.citapluus.FavoritePlace;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FavoritesApi {
    @GET("favoritos")
    Call<List<FavoritePlace>> getFavorites();

    @POST("favoritos")
    Call<Void> addFavorite(@Body FavoritePlace f);

    @DELETE("favoritos/{id}")
    Call<Void> deleteFavorite(@Path("id") String id);
}
