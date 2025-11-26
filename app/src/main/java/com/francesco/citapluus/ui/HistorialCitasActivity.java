package com.francesco.citapluus.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.francesco.citapluus.R;
import com.francesco.citapluus.CitaManager;
import java.util.List;

/**
 * Activity that displays the history of appointments (citas).
 * Users can view past appointments and filter by status.
 */
public class HistorialCitasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistorial;
    private ImageButton btnVolver;
    private Spinner spinnerFiltro;
    private CitaAdapter citaAdapter;
    private List<Cita> listaCitas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_citas);

        // Initialize views
        recyclerViewHistorial = findViewById(R.id.recyclerViewHistorial);
        btnVolver = findViewById(R.id.btnVolver);
        spinnerFiltro = findViewById(R.id.spinnerFiltro);

        // Setup back button
        btnVolver.setOnClickListener(v -> {
            finish(); // Return to MainActivity
        });

        // Setup RecyclerView
        recyclerViewHistorial.setLayoutManager(new LinearLayoutManager(this));

        // Get appointment history from CitaManager
        listaCitas = CitaManager.getInstance().getCitas();

        // Setup adapter
        citaAdapter = new CitaAdapter(listaCitas);
        recyclerViewHistorial.setAdapter(citaAdapter);

        // TODO: Setup spinner filter for different appointment statuses (all, completed, cancelled, etc.)
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the appointment list when returning to this screen
        listaCitas.clear();
        listaCitas.addAll(CitaManager.getInstance().getCitas());
        citaAdapter.notifyDataSetChanged();
    }
}

