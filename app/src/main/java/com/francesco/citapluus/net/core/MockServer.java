package com.francesco.citapluus.net.core;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public final class MockServer {
    private static MockWebServer server;

    private MockServer() {}

    public static synchronized void start() {
        if (server != null) return;
        try {
            server = new MockWebServer();
            server.setDispatcher(buildDispatcher());
            server.start(0); // puerto libre
        } catch (Throwable t) {
            t.printStackTrace();
            stop();
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            try { server.shutdown(); } catch (Throwable ignore) {}
            server = null;
        }
    }

    public static synchronized boolean isRunning() {
        return server != null;
    }

    /** ← ESTE MÉTODO FALTABA */
    public static synchronized String getBaseUrl() {
        if (server == null) throw new IllegalStateException("MockServer no iniciado");
        // Si tu API real cuelga de /api/, mantenlo así para que los endpoints coincidan
        return server.url("/api/").toString();
    }

    // --- Dispatcher de ejemplo (puedes ajustarlo a tus endpoints) ---
    private static Dispatcher buildDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                String method = request.getMethod();

                // Ejemplo para favoritos
                if (path != null && path.startsWith("/api/favorites")) {
                    if ("GET".equals(method)) {
                        String json = "[{\"id\":\"ph_1\",\"nombre\":\"Farmacia Centro\",\"direccion\":\"Calle A 123\",\"lat\":40.4169,\"lng\":-3.7037,\"tipo\":\"FARMACIA\"}]";
                        return json200(json);
                    }
                    if ("POST".equals(method) || "DELETE".equals(method)) {
                        return json200("{\"ok\":true}");
                    }
                }

                // Por defecto
                return new MockResponse().setResponseCode(404)
                        .setBody("{\"error\":\"mock route not found\"}");
            }
        };
    }

    private static MockResponse json200(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body != null ? body : "{}")
                .setBodyDelay(150, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
