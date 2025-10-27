package com.francesco.citapluus.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.francesco.citapluus.net.core.ApiService;
import com.francesco.citapluus.net.core.RetrofitProvider;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Envía cambios del perfil a la API si existe PATCH /me.
 * Si recibe 404, desactiva el remoto para esta sesión y no vuelve a intentarlo.
 * Siempre debes guardar localmente desde la Activity (SessionManager).
 */
public class ProfileRepository {

    private static final String TAG = "ProfileRepo";
    private static final String PREFS = "profile_repo_prefs";
    private static final String K_REMOTE_DISABLED = "remote_disabled_until"; // epoch millis
    // Evita reintentos por 24h si hay 404 (ajústalo si quieres)
    private static final long MUTE_MS = 24L * 60L * 60L * 1000L;

    private static ProfileRepository INSTANCE;

    public static synchronized ProfileRepository get(Context ctx) {
        if (INSTANCE == null) INSTANCE = new ProfileRepository(ctx.getApplicationContext());
        return INSTANCE;
    }

    private final Context app;
    private final ApiService api;
    private final SharedPreferences sp;

    private ProfileRepository(Context app) {
        this.app = app;
        this.api = RetrofitProvider.get(app).create(ApiService.class);
        this.sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Patchea un solo campo. No falla la UI: si 404, se desactiva remoto y listo. */
    public void patchField(@NonNull String field, @NonNull String value) {
        if (isRemoteMuted()) {
            Log.i(TAG, "remote muted; skip patch " + field);
            return;
        }
        api.patchMe(Collections.singletonMap(field, value))
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                        if (!resp.isSuccessful()) {
                            Log.w(TAG, "PATCH /me " + field + " -> HTTP " + resp.code());
                            if (resp.code() == 404) muteRemote();
                        } else {
                            Log.i(TAG, "PATCH /me " + field + " OK");
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.w(TAG, "PATCH /me " + field + " error", t);
                        // No muteamos por fallo de red; solo por 404
                    }
                });
    }

    private boolean isRemoteMuted() {
        long until = sp.getLong(K_REMOTE_DISABLED, 0L);
        return System.currentTimeMillis() < until;
    }

    private void muteRemote() {
        long until = System.currentTimeMillis() + MUTE_MS;
        sp.edit().putLong(K_REMOTE_DISABLED, until).apply();
        Log.w(TAG, "remote disabled until " + until);
    }
}
