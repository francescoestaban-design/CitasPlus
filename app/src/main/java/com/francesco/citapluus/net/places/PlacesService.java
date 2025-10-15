
package com.francesco.citapluus.net.places;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PlacesService {

    @GET("nearbysearch/json")
    Call<PlacesNearbyResponse> nearbySearch(
            @Query("location") String latlng,
            @Query("radius") int radiusMeters,
            @Query("type") String type,
            @Query("language") String lang,
            @Query("key") String apiKey
    );

    @GET("nearbysearch/json")
    Call<PlacesNearbyResponse> nearbySearchWithKeyword(
            @Query("location") String latlng,
            @Query("radius") int radiusMeters,
            @Query("type") String type,
            @Query("keyword") String keyword,
            @Query("language") String lang,
            @Query("key") String apiKey
    );
}
