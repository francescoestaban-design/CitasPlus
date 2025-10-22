package com.francesco.citapluus;

import android.app.Application;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.francesco.citapluus.net.core.MockServer;

public class App extends Application {
    private static PlacesClient placesClient;
    private static volatile boolean mockEnabled = false;
    private static volatile String mockBaseUrl = null;

    @Override public void onCreate() {
        super.onCreate();

        // Places
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        // Si estás depurando, levanta el mock en background para no violar StrictMode
        if (android.os.Debug.isDebuggerConnected()) {
            new Thread(() -> {
                try {
                    MockServer.start();
                    mockBaseUrl = MockServer.getBaseUrl();
                    mockEnabled = true;
                } catch (Throwable ignored) {
                    mockEnabled = false;
                }
            }, "MockServer-Init").start();
        }
    }

    public static PlacesClient getPlacesClient() { return placesClient; }
    public static boolean isMockEnabled() { return mockEnabled && mockBaseUrl != null; }
    public static String getMockBaseUrl() { return mockBaseUrl; }
}
