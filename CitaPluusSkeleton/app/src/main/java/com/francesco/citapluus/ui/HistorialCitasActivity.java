package com.francesco.citapluus.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.francesco.citapluus.CitaManager;
import com.francesco.citapluus.R;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialCitasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CitaAdapter adapter;
    private Spinner spinnerFiltro;
    private TextView emptyView;
    private MaterialButton buttonVolverMenu;
    
    private List<Cita> todasLasCitas = new ArrayList<>();
    private String filtroActual = "Todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_citas);

        // Configurar ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Historial de Citas");
        }

        recyclerView = findViewById(R.id.recyclerViewHistorial);
        spinnerFiltro = findViewById(R.id.spinnerFiltroEstado);
        emptyView = findViewById(R.id.emptyViewHistorial);
        buttonVolverMenu = findViewById(R.id.buttonVolverMenu);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Configurar spinner de filtros
        configurarFiltros();

        // Cargar citas
        cargarCitas();
        
        // Botón para volver al menú principal
        buttonVolverMenu.setOnClickListener(v -> finish());
    }

    private void configurarFiltros() {
        String[] filtros = {"Todas", "Pendientes", "Completadas", "Canceladas"};
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filtros);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterSpinner);

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtroActual = filtros[position];
                aplicarFiltro();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });
    }

    private void cargarCitas() {
        String dni = SessionManager.getInstance(this).getDNI();
        
        // Cargar desde Firestore
        CitaManager.getInstance().cargarCitasDesdeFirestore(dni, () -> {
            runOnUiThread(() -> {
                todasLasCitas = new ArrayList<>(CitaManager.getInstance().getCitas());
                
                // Ordenar por fecha (más recientes primero)
                Collections.sort(todasLasCitas, (c1, c2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date d1 = sdf.parse(c1.getFecha() + " " + c1.getHora());
                        Date d2 = sdf.parse(c2.getFecha() + " " + c2.getHora());
                        return d2.compareTo(d1); // Descendente (más recientes primero)
                    } catch (ParseException e) {
                        return 0;
                    }
                });
                
                // Actualizar estados automáticamente
                actualizarEstadosAutomaticamente();
                
                aplicarFiltro();
            });
        });
    }

    /**
     * Actualizar automáticamente citas pendientes → completadas si la fecha ya pasó
     */
    private void actualizarEstadosAutomaticamente() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Date ahora = new Date();
        
        for (Cita cita : todasLasCitas) {
            try {
                Date fechaCita = sdf.parse(cita.getFecha() + " " + cita.getHora());
                
                // Si la cita ya pasó y está pendiente, marcarla como completada
                if (fechaCita != null && fechaCita.before(ahora) && "pendiente".equals(cita.getEstado())) {
                    cita.setEstado("completada");
                    CitaManager.getInstance().actualizarCita(cita);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void aplicarFiltro() {
        List<Cita> citasFiltradas = new ArrayList<>();

        for (Cita cita : todasLasCitas) {
            String estado = cita.getEstado();
            if (estado == null) estado = "pendiente"; // Por si acaso hay citas antiguas sin estado

            switch (filtroActual) {
                case "Todas":
                    citasFiltradas.add(cita);
                    break;
                case "Pendientes":
                    if ("pendiente".equals(estado)) citasFiltradas.add(cita);
                    break;
                case "Completadas":
                    if ("completada".equals(estado)) citasFiltradas.add(cita);
                    break;
                case "Canceladas":
                    if ("cancelada".equals(estado)) citasFiltradas.add(cita);
                    break;
            }
        }

        // Mostrar/ocultar empty view
        if (citasFiltradas.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            String mensaje = "No hay citas " + filtroActual.toLowerCase();
            emptyView.setText(mensaje);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        // Configurar adapter
        adapter = new CitaAdapter(this, citasFiltradas);
        
        // Solo permitir ver detalles de citas completadas
        adapter.setOnItemClickListener(cita -> {
            if ("completada".equals(cita.getEstado())) {
                mostrarDetallesCita(cita);
            } else {
                Toast.makeText(this, "Solo puedes ver detalles de citas completadas", Toast.LENGTH_SHORT).show();
            }
        });
        
        recyclerView.setAdapter(adapter);
    }

    private void mostrarDetallesCita(Cita cita) {
        StringBuilder detalles = new StringBuilder();
        detalles.append("📅 Fecha: ").append(cita.getFecha()).append("\n\n");
        detalles.append("🕐 Hora: ").append(cita.getHora()).append("\n\n");
        detalles.append("👨‍⚕️ Doctor: ").append(cita.getDoctorNombre()).append("\n\n");
        detalles.append("📋 Motivo: ").append(cita.getMotivo()).append("\n\n");
        detalles.append("📍 Estado: ").append(capitalize(cita.getEstado())).append("\n\n");
        
        // Información del centro de salud
        if (cita.getCentroNombre() != null && !cita.getCentroNombre().isEmpty()) {
            detalles.append("🏥 Centro de Salud:\n");
            detalles.append("   ").append(cita.getCentroNombre()).append("\n");
            if (cita.getCentroDireccion() != null && !cita.getCentroDireccion().isEmpty()) {
                detalles.append("   ").append(cita.getCentroDireccion()).append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Detalles de la Cita")
                .setMessage(detalles.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

