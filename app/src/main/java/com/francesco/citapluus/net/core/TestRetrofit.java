package com.francesco.citapluus.net.core;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import java.util.List;

public class TestRetrofit extends AppCompatActivity {
    interface MockApi {
        @GET("/")
        Call<List<Object>> getMockData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MockApi api = RetrofitProvider.get(this).create(MockApi.class);
        api.getMockData().enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(Call<List<Object>> call, Response<List<Object>> response) {
                Log.d("RetrofitTest", "✅ Respuesta mock: " + response.body());
            }

            @Override
            public void onFailure(Call<List<Object>> call, Throwable t) {
                Log.e("RetrofitTest", "❌ Error: " + t.getMessage());
            }
        });
    }
}
