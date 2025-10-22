package com.francesco.citapluus.net.core;

import com.francesco.citapluus.App;

import android.content.Context;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProvider {
    private static volatile Retrofit instance;

    public static Retrofit get(Context ctx) {
        if (instance == null) {
            synchronized (RetrofitProvider.class) {
                if (instance == null) {
                    // Si el mock está habilitado (lo decidimos en App), usa su baseUrl
                    String baseUrl = App.isMockEnabled()
                            ? App.getMockBaseUrl()
                            : "https://mi-backend-real.com/api/";

                    OkHttpClient client = new OkHttpClient.Builder().build();

                    instance = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(client)
                            .build();
                }
            }
        }
        return instance;
    }
}
