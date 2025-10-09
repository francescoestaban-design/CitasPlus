package com.francesco.citapluus;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicamentosActivity extends AppCompatActivity
        implements MedicamentoAdapter.OnDrugClickListener {

    private RecyclerView rv;
    private MedicamentoAdapter adapter;

    // Catálogo base (se puede poblar desde red/BD más adelante)
    private final List<DrugInfo> base = new ArrayList<>();
    // Lista que se muestra (filtrada)
    private final List<DrugInfo> filtrados = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicamentos);

        // Título y flecha back en la barra
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Medicamentos");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 1) Cargar catálogo de ejemplo
        cargarEjemplos();

        // 2) RecyclerView
        rv = findViewById(R.id.recyclerMedicamentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicamentoAdapter(filtrados, this);
        rv.setAdapter(adapter);

        // 3) Mostrar todo al inicio
        filtrados.clear();
        filtrados.addAll(base);
        adapter.notifyDataSetChanged();

        // 4) Búsqueda
        SearchView sv = findViewById(R.id.searchMedicamentos);
        sv.setIconifiedByDefault(false);
        sv.setQueryHint("Buscar por nombre (p. ej. Paracetamol)");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filtrar(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filtrar(newText); return true; }
        });

        // 5) Botón “Volver al menú principal”
        findViewById(R.id.buttonVolverMenu).setOnClickListener(v -> finish());
    }

    private void cargarEjemplos() {
        base.clear();

        base.add(new DrugInfo(
                "Paracetamol",
                "Analgésico y antipirético. Se usa para dolor leve-moderado y fiebre.",
                "Adultos: 500–1000 mg cada 6–8 h (máx. 3 g/día salvo indicación médica).\n" +
                        "Niños: usar formulaciones pediátricas y dosis por peso (consulta médica).",
                "Evitar exceder la dosis; precaución en enfermedad hepática o consumo importante de alcohol.",
                "Generalmente bien tolerado; a dosis altas puede causar daño hepático."
        ));

        base.add(new DrugInfo(
                "Ibuprofeno",
                "Antiinflamatorio no esteroideo (AINE). Útil en dolor inflamatorio y fiebre.",
                "Adultos: 200–400 mg cada 6–8 h con comida (máx. 1.2 g/día sin receta).\n" +
                        "Niños: consultar dosis por peso.",
                "Evitar en úlcera activa, insuficiencia renal grave o embarazo avanzado; puede elevar riesgo gastrointestinal.",
                "Molestias gástricas, acidez; raramente úlcera o sangrado."
        ));

        base.add(new DrugInfo(
                "Amoxicilina",
                "Antibiótico penicilínico para infecciones bacterianas sensibles (otitis, sinusitis, etc.).",
                "Dosis y duración dependen del tipo de infección y criterio médico. No usar sin prescripción.",
                "No usar si hay alergia a penicilinas; completar tratamiento para evitar resistencias.",
                "Erupción cutánea, molestias digestivas; reacciones alérgicas en personas sensibles."
        ));
    }

    private void filtrar(String texto) {
        filtrados.clear();
        if (TextUtils.isEmpty(texto)) {
            filtrados.addAll(base);
        } else {
            String q = texto.trim().toLowerCase(Locale.ROOT);
            for (DrugInfo d : base) {
                if (d.getNombre().toLowerCase(Locale.ROOT).contains(q)) {
                    filtrados.add(d);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDrugClick(DrugInfo info) {
        String mensaje =
                "¿Para qué sirve?\n" + info.getParaQueSirve() + "\n\n" +
                        "¿Cómo tomarlo?\n" + info.getComoTomarlo() + "\n\n" +
                        "Advertencias\n" + info.getAdvertencias() + "\n\n" +
                        "Efectos secundarios\n" + info.getEfectosSecundarios() + "\n\n" +
                        "⚠️ Nota: Esta información es orientativa y no sustituye la consulta médica.";

        new MaterialAlertDialogBuilder(this)
                .setTitle(info.getNombre())
                .setMessage(mensaje)
                .setPositiveButton("Entendido", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
