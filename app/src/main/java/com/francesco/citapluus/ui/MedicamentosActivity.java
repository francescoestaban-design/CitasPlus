package com.francesco.citapluus.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;

import com.francesco.citapluus.App;
import com.francesco.citapluus.BuildConfig;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francesco.citapluus.DrugInfo;
import com.francesco.citapluus.MedicamentoAdapter;
import com.francesco.citapluus.R;
import com.francesco.citapluus.SuggestAdapter;
import com.francesco.citapluus.net.core.ApiService;
import com.francesco.citapluus.net.core.CimaApi;
import com.francesco.citapluus.net.core.CimaMedicineRepository;
import com.francesco.citapluus.net.core.CimaRetrofitProvider;
import com.francesco.citapluus.net.core.HybridMedicineRepository;
import com.francesco.citapluus.net.core.MedicineRepository;
import com.francesco.citapluus.net.core.RetrofitProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicamentosActivity extends AppCompatActivity
        implements MedicamentoAdapter.OnDrugClickListener {

    private static final String TAG = "Medicamentos";

    private RecyclerView rv;
    private MedicamentoAdapter adapter;

    private RecyclerView rvSugg;
    private SuggestAdapter suggestAdapter;

    private final List<com.francesco.citapluus.DrugInfo> base = new ArrayList<>();
    private final List<com.francesco.citapluus.DrugInfo> filtrados = new ArrayList<>();
    private final List<String> sugerencias = new ArrayList<>();

    private MedicineRepository repository;

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingSuggest;

    private TextView debugInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        android.util.Log.d(TAG, "Repo = " + (BuildConfig.USE_MOCK ? "MOCK/HYBRID" : "CIMA"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicamentos);
        debugInfo = findViewById(R.id.debugInfo);
        updateDebug("🔹 Base URL: " + App.getEffectiveBaseUrl());


        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Medicamentos");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Repo
        ApiService mockApi = RetrofitProvider.get(this).create(ApiService.class);
        CimaApi    cimaApi = CimaRetrofitProvider.get().create(CimaApi.class);
        repository = BuildConfig.USE_MOCK
                ? new HybridMedicineRepository(mockApi, cimaApi)
                : new CimaMedicineRepository(cimaApi);

        // Lista resultados
        rv = findViewById(R.id.recyclerMedicamentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicamentoAdapter(filtrados, this);
        rv.setAdapter(adapter);

        // Lista sugerencias (debajo del buscador)
        rvSugg = findViewById(R.id.recyclerSugerencias);
        rvSugg.setLayoutManager(new LinearLayoutManager(this));
        suggestAdapter = new SuggestAdapter(texto -> {
            // Al pulsar sugerencia: rellenamos query y buscamos
            SearchView sv = findViewById(R.id.searchMedicamentos);
            sv.setQuery(texto, false);
            ocultarSugerencias();
            buscar(texto);
        });
        rvSugg.setAdapter(suggestAdapter);

        // Carga inicial
        buscar(null);

        // SearchView + debounce + suggest
        SearchView sv = findViewById(R.id.searchMedicamentos);
        sv.setIconifiedByDefault(false);
        sv.setQueryHint("Buscar por nombre (p. ej. Paracetamol)");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) {
                android.util.Log.d(TAG, "submit=" + q);
                ocultarSugerencias();
                buscar(q);
                return true;
            }
            @Override public boolean onQueryTextChange(String t) {
                if (pendingSuggest != null) handler.removeCallbacks(pendingSuggest);
                final String texto = t == null ? "" : t.trim();
                pendingSuggest = () -> {
                    if (texto.length() < 2) {
                        android.util.Log.d(TAG, "suggest skip (len<2)");
                        ocultarSugerencias();
                        buscar(null); // muestra todo
                        return;
                    }
                    android.util.Log.d(TAG, "suggest pedir -> " + texto);
                    repository.suggest(texto, new MedicineRepository.SuggestCallback() {
                        @Override public void onSuccess(List<String> nombres) {
                            List<String> list = nombres != null ? nombres : new ArrayList<>();
                            // Fallback local: añade coincidencias de lo ya cargado
                            for (com.francesco.citapluus.DrugInfo d : base) {
                                if (d.getNombre() != null &&
                                        d.getNombre().toLowerCase(Locale.ROOT)
                                                .startsWith(texto.toLowerCase(Locale.ROOT))) {
                                    if (!list.contains(d.getNombre())) list.add(d.getNombre());
                                }
                            }
                            actualizarSugerencias(list);
                        }
                        @Override public void onError(Throwable t) {
                            android.util.Log.w(TAG, "suggest error", t);
                            // solo fallback local
                            List<String> list = new ArrayList<>();
                            for (com.francesco.citapluus.DrugInfo d : base) {
                                if (d.getNombre() != null &&
                                        d.getNombre().toLowerCase(Locale.ROOT)
                                                .startsWith(texto.toLowerCase(Locale.ROOT))) {
                                    list.add(d.getNombre());
                                }
                            }
                            actualizarSugerencias(list);
                        }
                    });
                };
                handler.postDelayed(pendingSuggest, 220); // feeling más “natural”
                return true;
            }
        });

        findViewById(R.id.buttonVolverMenu).setOnClickListener(v -> finish());
    }
    private void updateDebug(String msg) {
        if (debugInfo != null) {
            runOnUiThread(() -> debugInfo.setText(msg));
        }
    }
    private void actualizarSugerencias(List<String> lista) {
        sugerencias.clear();
        if (lista != null) sugerencias.addAll(lista);
        suggestAdapter.setData(sugerencias);
        rvSugg.setVisibility(sugerencias.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void ocultarSugerencias() {
        sugerencias.clear();
        suggestAdapter.clear();
        rvSugg.setVisibility(android.view.View.GONE);
    }

    private void buscar(String query) {
        android.util.Log.d(TAG, "buscar -> " + (query == null ? "__all__" : query));
        repository.search(query, new MedicineRepository.Callback() {
            @Override public void onSuccess(List<com.francesco.citapluus.DrugInfo> items) {
                runOnUiThread(() -> {
                    base.clear();
                    base.addAll(items);
                    filtrar(null);
                });
            }
            @Override public void onError(Throwable t) {
                runOnUiThread(() ->
                        new MaterialAlertDialogBuilder(MedicamentosActivity.this)
                                .setTitle("Error")
                                .setMessage("No se pudo cargar el catálogo.\n" + t.getMessage())
                                .setPositiveButton("OK", null)
                                .show()
                );
            }
        });
    }

    private void filtrar(String texto) {
        filtrados.clear();
        if (TextUtils.isEmpty(texto)) {
            filtrados.addAll(base);
        } else {
            String q = texto.trim().toLowerCase(Locale.ROOT);
            for (com.francesco.citapluus.DrugInfo d : base) {
                if (d.getNombre().toLowerCase(Locale.ROOT).contains(q)) {
                    filtrados.add(d);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDrugClick(DrugInfo info) {
        SessionManager sm = SessionManager.getInstance(this);
        List<String> alergias = sm.getAlergiasList();

        List<String> inter = new ArrayList<>();
        for (String a : info.getAlergenos()) {
            for (String u : alergias) if (a.equalsIgnoreCase(u)) inter.add(a);
        }

        String extra = inter.isEmpty() ? "" :
                "\n\n⚠️ Posible conflicto con tus alergias: " + inter;

        String mensaje =
                "¿Para qué sirve?\n" + info.getParaQueSirve() + "\n\n" +
                        "¿Cómo tomarlo?\n" + info.getComoTomarlo() + "\n\n" +
                        "Advertencias\n" + info.getAdvertencias() + "\n\n" +
                        "Efectos secundarios\n" + info.getEfectosSecundarios() +
                        extra + "\n\n" +
                        "Nota: Información orientativa. Consulta a tu médico.";

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle(info.getNombre())
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null);

        String url = (info.getUrlProspecto() != null && !info.getUrlProspecto().trim().isEmpty())
                ? info.getUrlProspecto() : info.getUrlFichaTecnica();

        b.setNeutralButton("Ver prospecto/FT",
                (dlg, which) -> openInBrowser(url, info.getNombre()));
        b.show();
    }

    private void openInBrowser(String url, String nombreMedicamento) {
        try {
            String finalUrl = url;

            // Si la URL del mock está vacía o es inválida, generamos el link de búsqueda real
            if (finalUrl == null || finalUrl.trim().isEmpty() ||
                    !(finalUrl.startsWith("http://") || finalUrl.startsWith("https://"))) {
                String q = URLEncoder.encode(
                        nombreMedicamento == null ? "" : nombreMedicamento, "UTF-8");
                finalUrl = "https://cima.aemps.es/cima/publico/lista.html?nombre=" + q;
            }

            // Log detallado para saber qué URL abre
            android.util.Log.i("Medicamentos", "🌐 Abriendo en navegador -> " + finalUrl);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("Medicamentos", "Error abriendo navegador", e);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Error al abrir CIMA")
                    .setMessage("No se pudo abrir el enlace del medicamento.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
