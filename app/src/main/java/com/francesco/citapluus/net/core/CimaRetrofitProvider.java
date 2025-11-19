package com.francesco.citapluus.net.core;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public final class CimaRetrofitProvider {
    private static Retrofit retrofit;

    private static final String BASE_URL = "https://cima.aemps.es/cima/rest/";

    private CimaRetrofitProvider() {}

    public static synchronized Retrofit get() {
        if (retrofit != null) return retrofit;

        // Nivel de logging más detallado
        HttpLoggingInterceptor log = new HttpLoggingInterceptor(message ->
                android.util.Log.i("CIMA.OkHttp", message));
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(log)
                .readTimeout(25, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
