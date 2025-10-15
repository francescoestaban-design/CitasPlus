package com.francesco.citapluus.net.places;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitPlaces {
    private static Retrofit instance;

    public static Retrofit get() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl("https://maps.googleapis.com/maps/api/place/")
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }

    private RetrofitPlaces() {}
}
