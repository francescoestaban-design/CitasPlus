package com.francesco.citapluus;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.francesco.citapluus.net.core.MockServer;
import com.francesco.citapluus.net.core.RetrofitProvider;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

/**
 * App: inicializa Preferencias, Places y (opcional) MockWebServer.
 * Filtra en Logcat:  tag:NetworkCfg
 */
public class App extends Application {

    private static App instance;

    // Estado global de red
    private static volatile boolean mockEnabled = false;        // switch UI
    private static volatile String  realBaseUrl = "https://mi-backend-real.com/api/";
    private static volatile String  mockBaseUrl = null;         // se setea cuando arranca el mock

    private static PlacesClient placesClient;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // === Preferencias iniciales ===
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // por defecto: mock activado en DEBUG, desactivado en RELEASE
        mockEnabled = sp.getBoolean("pref_mock_enabled", BuildConfig.DEBUG);
        realBaseUrl = sp.getString("pref_real_base_url", realBaseUrl);

        // Log de arranque
        Log.i("NetworkCfg",
                "AppStart  mockSwitch=" + mockEnabled +
                        "  realBase=" + realBaseUrl +
                        "  isDebug=" + BuildConfig.DEBUG);

        // === Places (siempre que uses Places en la app) ===
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        // === Arrancar Mock SOLO si el usuario lo activó y estamos en Debug ===
        if (mockEnabled && BuildConfig.DEBUG) {
            MockServer.startAsync(new MockServer.Callback() {
                @Override public void onStarted(String baseUrl) {
                    mockBaseUrl = baseUrl;
                    Log.i("NetworkCfg", "MockServer started at " + baseUrl);
                }
                @Override public void onFailed(Throwable t) {
                    mockBaseUrl = null;
                    Log.e("NetworkCfg", "MockServer failed: " + t);
                }
            });
        }
    }

    // === API expuesta a toda la app ===
    public static boolean isMockEnabled() {
        // en Release nunca permitimos Mock (protección adicional)
        return BuildConfig.DEBUG && mockEnabled;
    }

    public static void setMockEnabled(boolean enabled) {
        mockEnabled = enabled;
        PreferenceManager.getDefaultSharedPreferences(instance)
                .edit().putBoolean("pref_mock_enabled", enabled).apply();
        Log.i("NetworkCfg", "setMockEnabled=" + enabled);
    }

    public static String getRealBaseUrl() { return realBaseUrl; }

    public static void setRealBaseUrl(String url) {
        realBaseUrl = url;
        PreferenceManager.getDefaultSharedPreferences(instance)
                .edit().putString("pref_real_base_url", url).apply();
        Log.i("NetworkCfg", "setRealBaseUrl=" + url);
    }

    /** URL efectiva que usará Retrofit ahora mismo */
    public static String getEffectiveBaseUrl() {
        String effective = (isMockEnabled() && mockBaseUrl != null) ? mockBaseUrl : realBaseUrl;
        Log.i("NetworkCfg", "getEffectiveBaseUrl -> " + effective +
                " (mockEnabled=" + isMockEnabled() + ", mockBase=" + mockBaseUrl + ")");
        return effective;
    }

    /** Llamar cuando cambie el switch o la URL real para reconstruir Retrofit */
    public static void notifyNetworkConfigChanged() {
        Log.i("NetworkCfg", "notifyNetworkConfigChanged -> resetting Retrofit");
        RetrofitProvider.reset();
    }

    public static PlacesClient getPlacesClient() { return placesClient; }

    public static App get() { return instance; }
}
