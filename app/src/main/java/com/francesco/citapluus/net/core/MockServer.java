package com.francesco.citapluus.net.core;

import com.francesco.citapluus.FavoritePlace;
import com.francesco.citapluus.net.dto.DrugInfoDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public final class MockServer {
    private static MockWebServer server;
    private static final Gson GSON = new Gson();

    // ===== Estado simulado en memoria =====
    private static final List<FavoritePlace> MEMORY_FAVS = new CopyOnWriteArrayList<>();
    // Perfil simple
    private static final Map<String, String> PROFILE = new ConcurrentHashMap<>();
    // Catálogo de medicamentos (con alérgenos)
    private static final List<DrugInfoDto> MEDICINES = new CopyOnWriteArrayList<>();

    private MockServer() {}

    public static synchronized void start() {
        if (server != null) return;
        try {
            seed();
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
        // Importante mantener el sufijo /api/ porque RetrofitProvider apunta ahí
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

    // ===== Datos de ejemplo =====
    private static void seed() {
        // Favorito demo
        if (MEMORY_FAVS.isEmpty()) {
            FavoritePlace demo = new FavoritePlace(
                    "ph_1", "Farmacia Centro", "Calle A 123",
                    40.4169, -3.7037, "FARMACIA"
            );
            MEMORY_FAVS.add(demo);
        }
        // Perfil demo
        PROFILE.putIfAbsent("tipoSangre", "A+");
        PROFILE.putIfAbsent("alergias", "penicilina"); // CSV normalizado (minúsculas, coma)
        PROFILE.putIfAbsent("codigoPostal", "28001");

        // Catálogo demo
        if (MEDICINES.isEmpty()) {
            DrugInfoDto d1 = new DrugInfoDto();
            d1.nombre = "Paracetamol";
            d1.paraQueSirve = "Analgésico y antipirético.";
            d1.comoTomarlo = "Adultos: 500–1000 mg cada 6–8 h (máx. 3 g/día salvo indicación médica).";
            d1.advertencias = "Precaución en enfermedad hepática o consumo importante de alcohol.";
            d1.efectosSecundarios = "Generalmente bien tolerado; a dosis altas hepatotoxicidad.";
            d1.alergenos = Arrays.asList("lactosa"); // ejemplo

            DrugInfoDto d2 = new DrugInfoDto();
            d2.nombre = "Ibuprofeno";
            d2.paraQueSirve = "AINE para dolor inflamatorio y fiebre.";
            d2.comoTomarlo = "200–400 mg cada 6–8 h con comida (máx. 1.2 g/día sin receta).";
            d2.advertencias = "Evitar en úlcera activa o insuficiencia renal grave.";
            d2.efectosSecundarios = "Molestias gástricas, acidez; raramente úlcera.";
            d2.alergenos = Arrays.asList("ibuprofeno", "aine", "salicilatos");

            DrugInfoDto d3 = new DrugInfoDto();
            d3.nombre = "Amoxicilina";
            d3.paraQueSirve = "Antibiótico penicilínico.";
            d3.comoTomarlo = "Según infección y prescripción médica.";
            d3.advertencias = "No usar si hay alergia a penicilinas/β-lactámicos.";
            d3.efectosSecundarios = "Erupción cutánea, molestias digestivas; reacciones alérgicas.";
            d3.alergenos = Arrays.asList("penicilina", "amoxicilina", "beta-lactamico");

            MEDICINES.add(d1);
            MEDICINES.add(d2);
            MEDICINES.add(d3);

            d1.urlProspecto = "https://cima.aemps.es/cima/dochtml/p/54925/P_54925.html";
            d1.urlFichaTecnica = "https://cima.aemps.es/cima/dochtml/ft/54925/FT_54925.html";

            d2.urlProspecto = "https://cima.aemps.es/cima/dochtml/p/58495/P_58495.html";
            d2.urlFichaTecnica = "https://cima.aemps.es/cima/dochtml/ft/58495/FT_58495.html";

            d3.urlProspecto = "https://cima.aemps.es/cima/dochtml/p/56062/P_56062.html";
            d3.urlFichaTecnica = "https://cima.aemps.es/cima/dochtml/ft/56062/FT_56062.html";


        }
    }

    private static Dispatcher buildDispatcher() {
        return new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest req) {
                String path = req.getPath();   // incluye query string
                String method = req.getMethod();

                // ---- PING ----
                if ("/api/ping".equals(path)) {
                    return json(200, "{\"msg\":\"pong-mock\"}");
                }

                // ---- FAVORITES ----
                if (path != null && path.startsWith("/api/favorites")) {
                    if ("GET".equals(method) && "/api/favorites".equals(path)) {
                        String json = GSON.toJson(MEMORY_FAVS,
                                new TypeToken<List<FavoritePlace>>(){}.getType());
                        return json(200, json);
                    }
                    if ("POST".equals(method) && "/api/favorites".equals(path)) {
                        try {
                            String body = req.getBody().readUtf8();
                            FavoritePlace fav = GSON.fromJson(body, FavoritePlace.class);
                            if (fav != null && fav.id != null) {
                                MEMORY_FAVS.removeIf(f -> f.id.equals(fav.id)); // upsert simple
                                MEMORY_FAVS.add(fav);
                                return json(200, GSON.toJson(fav));
                            }
                            return json(400, "{\"error\":\"invalid body\"}");
                        } catch (Throwable t) {
                            return json(400, "{\"error\":\"bad json\"}");
                        }
                    }
                    if ("DELETE".equals(method)) {
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

                // ---- PERFIL (/api/me) ----
                if ("/api/me".equals(path) && "GET".equals(method)) {
                    return json(200, GSON.toJson(PROFILE));
                }

                if ("/api/me".equals(path) && "PATCH".equals(method)) {
                    try {
                        String body = req.getBody().readUtf8();
                        Map<String, String> patch = GSON.fromJson(
                                body, new TypeToken<Map<String, String>>(){}.getType());
                        if (patch == null) patch = new java.util.HashMap<>();

                        if (patch.containsKey("tipoSangre"))   PROFILE.put("tipoSangre",   safe(patch.get("tipoSangre")));
                        if (patch.containsKey("alergias"))     PROFILE.put("alergias",     normalizeCsv(patch.get("alergias")));
                        if (patch.containsKey("codigoPostal")) PROFILE.put("codigoPostal", safe(patch.get("codigoPostal")));

                        return noContent();
                    } catch (Throwable t) {
                        return json(400, "{\"error\":\"bad json\"}");
                    }
                }

                // GET /api/me/allergies -> lista normalizada
                if ("/api/me/allergies".equals(path) && "GET".equals(method)) {
                    List<String> list = splitCsv(PROFILE.getOrDefault("alergias",""));
                    return json(200, GSON.toJson(list));
                }

                // ---- MEDICINES (/api/medicines?q=...) ----
                if (path != null && path.startsWith("/api/medicines") && "GET".equals(method)) {
                    String q = getQueryParam(path, "q");
                    List<DrugInfoDto> out = new ArrayList<>();
                    if (q == null || q.trim().isEmpty()) {
                        out.addAll(MEDICINES);
                    } else {
                        String s = q.trim().toLowerCase(Locale.ROOT);
                        for (DrugInfoDto d : MEDICINES) {
                            if (d.nombre != null && d.nombre.toLowerCase(Locale.ROOT).contains(s)) {
                                out.add(d);
                            }
                        }
                    }
                    return json(200, GSON.toJson(out, new TypeToken<List<DrugInfoDto>>(){}.getType()));
                }// dentro del Dispatcher, justo antes del 404:
                if (path != null && path.startsWith("/api/medicines/suggest") && "GET".equals(method)) {
                    String q = getQueryParam(path, "q");
                    java.util.List<String> out = new java.util.ArrayList<>();
                    if (q != null) {
                        String s = q.trim().toLowerCase(java.util.Locale.ROOT);
                        for (com.francesco.citapluus.net.dto.DrugInfoDto d : MEDICINES) {
                            if (d.nombre != null && d.nombre.toLowerCase(java.util.Locale.ROOT).contains(s)) {
                                out.add(d.nombre);
                            }
                        }
                    }
                    return json(200, GSON.toJson(out, new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType()));
                }
                return json(404, "{\"error\":\"mock route not found\"}");
            }
        };
    }

    // ===== Helpers =====
    private static String safe(String s) { return s == null ? "" : s; }

    private static String normalizeCsv(String s) {
        // " Penicilina ,  ibuprofeno" -> "penicilina,ibuprofeno"
        List<String> parts = splitCsv(s);
        return String.join(",", parts);
    }

    private static List<String> splitCsv(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return out;
        for (String p : s.split(",")) {
            String t = p.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String getQueryParam(String fullPath, String key) {
        int i = fullPath.indexOf('?');
        if (i < 0) return null;
        String[] pairs = fullPath.substring(i + 1).split("&");
        for (String kv : pairs) {
            int j = kv.indexOf('=');
            if (j > 0) {
                String k = kv.substring(0, j);
                String v = kv.substring(j + 1);
                if (k.equals(key)) {
                    try {
                        return URLDecoder.decode(v, "UTF-8"); // ✅ compatible con minSdk 24
                    } catch (Exception e) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body != null ? body : "{}")
                .setBodyDelay(120, TimeUnit.MILLISECONDS);
    }

    private static MockResponse noContent() {
        return new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBodyDelay(100, TimeUnit.MILLISECONDS);
    }
}
