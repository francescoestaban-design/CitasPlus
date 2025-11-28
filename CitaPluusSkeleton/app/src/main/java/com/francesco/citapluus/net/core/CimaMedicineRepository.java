package com.francesco.citapluus.net.core;

import android.util.Log;
import com.francesco.citapluus.DrugInfo;
import com.francesco.citapluus.net.dto.cima.CimaMedicamentoDetalle;
import com.francesco.citapluus.net.dto.cima.CimaMedicamentoResponse;
import com.francesco.citapluus.net.dto.cima.CimaMedicamentoResumen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class CimaMedicineRepository implements MedicineRepository {
    private static final String TAG = "CIMARepo";
    private final CimaApi api;

    public CimaMedicineRepository(CimaApi api) {
        this.api = api;
    }

    @Override
    public void search(String query, MedicineRepository.Callback cb) {
        String q = (query == null ? "" : query.trim());
        if (q.length() < 2) {
            cb.onSuccess(Collections.emptyList());
            return;
        }

        Log.d(TAG, "CIMA search: '" + q + "'");
        api.search(q).enqueue(new retrofit2.Callback<CimaMedicamentoResponse>() {
            @Override
            public void onResponse(Call<CimaMedicamentoResponse> call, Response<CimaMedicamentoResponse> resp) {
                Log.d(TAG, "CIMA response code: " + resp.code());
                if (!resp.isSuccessful() || resp.body() == null || resp.body().resultados == null) {
                    Log.w(TAG, "CIMA error: HTTP " + resp.code());
                    cb.onError(new RuntimeException("HTTP " + resp.code()));
                    return;
                }
                Log.d(TAG, "CIMA resultados: " + resp.body().resultados.size());

                List<CimaMedicamentoResumen> lista = resp.body().resultados;
                if (lista.isEmpty()) {
                    cb.onSuccess(Collections.emptyList());
                    return;
                }

                List<DrugInfo> out = new ArrayList<>();
                HashMap<String, Integer> indexByNreg = new HashMap<>();

                for (CimaMedicamentoResumen r : lista) {
                    DrugInfo d = new DrugInfo(
                            safe(r.nombre),
                            "Laboratorio: " + safe(r.labtitular),
                            "Prescripción: " + safe(r.cpresc),
                            "Consulta la ficha técnica o el prospecto.",
                            "Uso: " + (r.vtm != null ? safe(r.vtm.nombre) : "Ver prospecto."),
                            new ArrayList<>()
                    );
                    out.add(d);
                    if (r.nregistro != null && !r.nregistro.trim().isEmpty()) {
                        indexByNreg.put(r.nregistro.trim(), out.size() - 1);
                    }
                }

                if (indexByNreg.isEmpty()) {
                    cb.onSuccess(out);
                    return;
                }

                final int total = indexByNreg.size();
                final int[] done = {0};

                for (String nreg : indexByNreg.keySet()) {
                    api.detalle(nreg).enqueue(new retrofit2.Callback<CimaMedicamentoDetalle>() {
                        @Override
                        public void onResponse(Call<CimaMedicamentoDetalle> call, Response<CimaMedicamentoDetalle> resp2) {
                            try {
                                Integer idx = indexByNreg.get(nreg);
                                if (idx == null || idx < 0 || idx >= out.size()) return;
                                DrugInfo ui = out.get(idx);

                                if (resp2.code() == 200 && resp2.body() != null) {
                                    // ✅ Detalle JSON válido → procesar documentos
                                    CimaMedicamentoDetalle d = resp2.body();
                                    String urlPros = null;
                                    String urlFt = null;

                                    if (d.docs != null) {
                                        for (CimaMedicamentoDetalle.Documento doc : d.docs) {
                                            String tipo = safe(doc.tipo).toLowerCase();
                                            String secc = safe(doc.seccion).toLowerCase();

                                            boolean esProspecto = tipo.contains("prospecto") || secc.contains("prospecto");
                                            boolean esFT = tipo.contains("ficha") || tipo.contains("técnica")
                                                    || secc.contains("ficha") || secc.contains("técnica");

                                            if (esProspecto && urlPros == null) urlPros = fixUrl(doc.url);
                                            if (esFT && urlFt == null) urlFt = fixUrl(doc.url);
                                        }
                                    }

                                    if (urlPros != null) ui.setUrlProspecto(urlPros);
                                    if (urlFt != null) ui.setUrlFichaTecnica(urlFt);

                                } else {
                                    // 🚧 Si el detalle no existe, crear un fallback a la web pública
                                    Log.w(TAG, "Detalle no disponible para " + nreg + " (HTTP " + resp2.code() + ")");
                                    String fallback = "https://cima.aemps.es/cima/publico/lista.html?nombre="
                                            + ui.getNombre().replace(" ", "%20");
                                    ui.setUrlProspecto(fallback);
                                    ui.setUrlFichaTecnica(fallback);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando detalle " + nreg, e);
                            } finally {
                                if (++done[0] == total) cb.onSuccess(out);
                            }
                        }

                        @Override
                        public void onFailure(Call<CimaMedicamentoDetalle> call, Throwable t) {
                            Log.w(TAG, "detalle fail " + nreg, t);
                            if (++done[0] == total) cb.onSuccess(out);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<CimaMedicamentoResponse> call, Throwable t) {
                Log.e(TAG, "CIMA search FAILED: " + t.getMessage(), t);
                cb.onError(t);
            }
        });
    }

    @Override
    public void suggest(String query, MedicineRepository.SuggestCallback cb) {
        String q = (query == null ? "" : query.trim());
        if (q.isEmpty()) {
            cb.onSuccess(Collections.emptyList());
            return;
        }

        Log.d(TAG, "CIMA suggest: '" + q + "'");
        api.search(q).enqueue(new retrofit2.Callback<CimaMedicamentoResponse>() {
            @Override
            public void onResponse(Call<CimaMedicamentoResponse> call, Response<CimaMedicamentoResponse> resp) {
                Log.d(TAG, "CIMA suggest response code: " + resp.code());
                if (!resp.isSuccessful() || resp.body() == null || resp.body().resultados == null) {
                    Log.w(TAG, "CIMA suggest error: HTTP " + resp.code());
                    cb.onError(new RuntimeException("HTTP " + resp.code()));
                    return;
                }

                List<String> nombres = new ArrayList<>();
                for (CimaMedicamentoResumen r : resp.body().resultados) {
                    if (r != null && r.nombre != null)
                        nombres.add(r.nombre);
                }
                Log.d(TAG, "CIMA suggest resultados: " + nombres.size());
                cb.onSuccess(nombres);
            }

            @Override
            public void onFailure(Call<CimaMedicamentoResponse> call, Throwable t) {
                Log.e(TAG, "CIMA suggest FAILED: " + t.getMessage(), t);
                cb.onError(t);
            }
        });
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String fixUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://cima.aemps.es" + (url.startsWith("/") ? "" : "/") + url;
    }
}
