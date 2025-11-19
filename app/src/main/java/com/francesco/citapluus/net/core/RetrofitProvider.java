package com.francesco.citapluus.net.core;

import android.content.Context;
import android.util.Log;

import com.francesco.citapluus.App;
import com.francesco.citapluus.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProvider {

    private static volatile Retrofit instance;

    public static synchronized void reset() { instance = null; }

    public static Retrofit get(Context ctx) {
        if (instance == null) {
            OkHttpClient.Builder okb = new OkHttpClient.Builder();

            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor log = new HttpLoggingInterceptor(message ->
                        Log.i("Mock.OkHttp", message));
                log.setLevel(HttpLoggingInterceptor.Level.BODY);
                okb.addInterceptor(log);
            }

            String baseUrl = App.getEffectiveBaseUrl();
            Log.i("NetworkCfg", "Usando baseUrl=" + baseUrl +
                    "  mock=" + App.isMockEnabled() +
                    "  debug=" + BuildConfig.DEBUG);

            instance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okb.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }
}
