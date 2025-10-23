package com.francesco.citapluus.net.core;

import com.francesco.citapluus.FavoritePlace;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public final class MockServer {
    private static MockWebServer server;
    private static final Gson GSON = new Gson();

    // estado en memoria (simula BD del backend)
    private static final List<FavoritePlace> MEMORY_FAVS = new CopyOnWriteArrayList<>();

    private MockServer() {}

    public static synchronized void start() {
        if (server != null) return;
        try {
            // seed inicial (si quieres más, agrega)
            if (MEMORY_FAVS.isEmpty()) {
                FavoritePlace demo = new FavoritePlace(
                        "ph_1", "Farmacia Centro", "Calle A 123",
                        40.4169, -3.7037, "FARMACIA"
                );
                MEMORY_FAVS.add(demo);
            }

            server = new MockWebServer();
            server.setDispatcher(buildDispatcher());
            server.start(0);
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

    public static synchronized boolean isRunning() { return server != null; }

    public static synchronized String getBaseUrl() {
        if (server == null) throw new IllegalStateException("MockServer no iniciado");
        return server.url("/api/").toString();
    }

    public static void startAsync(Callback cb) {
        if (server != null) {
            if (cb != null) cb.onStarted(getBaseUrl());
            return;
        }
        new Thread(() -> {
            try {
                start();
                if (cb != null) cb.onStarted(getBaseUrl());
            } catch (Throwable t) {
                if (cb != null) cb.onFailed(t);
            }
        }, "mock-server").start();
    }

    public interface Callback {
        void onStarted(String baseUrl);
        void onFailed(Throwable t);
    }

    private static Dispatcher buildDispatcher() {
        return new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest req) {
                String path = req.getPath();     // incluye query
                String method = req.getMethod();

                // --- Ping ---
                if ("/api/ping".equals(path)) {
                    return json(200, "{\"msg\":\"pong-mock\"}");
                }

                // --- Favoritos ---
                if (path != null && path.startsWith("/api/favorites")) {
                    // GET /api/favorites
                    if ("GET".equals(method) && "/api/favorites".equals(path)) {
                        String json = GSON.toJson(MEMORY_FAVS,
                                new TypeToken<List<FavoritePlace>>(){}.getType());
                        return json(200, json);
                    }

                    // POST /api/favorites  (cuerpo = FavoritePlace)
                    if ("POST".equals(method) && "/api/favorites".equals(path)) {
                        try {
                            String body = req.getBody().readUtf8();
                            FavoritePlace fav = GSON.fromJson(body, FavoritePlace.class);
                            if (fav != null && fav.id != null) {
                                // upsert sencillo: quita si existe y agrega
                                MEMORY_FAVS.removeIf(f -> f.id.equals(fav.id));
                                MEMORY_FAVS.add(fav);
                                return json(200, GSON.toJson(fav));
                            }
                            return json(400, "{\"error\":\"invalid body\"}");
                        } catch (Throwable t) {
                            return json(400, "{\"error\":\"bad json\"}");
                        }
                    }

                    // DELETE /api/favorites/{id}
                    if ("DELETE".equals(method)) {
                        // path ej: /api/favorites/ph_1
                        String[] parts = path.split("/");
                        if (parts.length == 4) {
                            String id = parts[3];
                            boolean removed = MEMORY_FAVS.removeIf(f -> f.id.equals(id));
                            if (removed) return json(200, "{}");
                            return json(404, "{\"error\":\"not found\"}");
                        }
                        return json(400, "{\"error\":\"bad id\"}");
                    }
                }

                // por defecto
                return json(404, "{\"error\":\"mock route not found\"}");
            }
        };
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body != null ? body : "{}")
                .setBodyDelay(120, TimeUnit.MILLISECONDS);
    }
}
