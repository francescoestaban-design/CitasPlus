package com.francesco.citapluus.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.francesco.citapluus.R;
import com.google.android.material.card.MaterialCardView;
import com.francesco.citapluus.ui.PerfilActivity;

import java.util.Calendar;

/**
 * Pantalla principal del paciente.
 * Desde aquí el usuario puede acceder a:
 * - Citas
 * - Medicamentos
 * - Historial
 * - Centro de salud
 * - Perfil
 * - Emergencia
 */
public class MainActivity extends AppCompatActivity {

    // Header (bienvenida y centro de salud)
    private TextView textViewTitulo;
    private TextView tvCentroResumen;

    // Tarjetas principales del menú
    private MaterialCardView tileCitas, tileMedicamentos, tileHistorial,
            tileCentro, tilePerfil, tileEmergencia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_paciente); // ✅ Layout del menú principal

        // ==== Referencias del header ====
        textViewTitulo = findViewById(R.id.textViewTitulo);
        tvCentroResumen = findViewById(R.id.tvCentroResumen);

        // ==== Referencias de las tarjetas ====
        tileCitas = findViewById(R.id.tileCitas);
        tileMedicamentos = findViewById(R.id.tileMedicamentos);
        tileHistorial = findViewById(R.id.tileHistorial);
        tileCentro = findViewById(R.id.tileCentro);
        tilePerfil = findViewById(R.id.tilePerfil);
        tileEmergencia = findViewById(R.id.tileEmergencia);

        // ==== Mostrar nombre del usuario ====
        String nombreUsuario = getIntent().getStringExtra("usuario");
        textViewTitulo.setText(
                (nombreUsuario != null && !nombreUsuario.isEmpty())
                        ? "Bienvenido, " + nombreUsuario
                        : "Bienvenido, Paciente"
        );

        // ==== Mostrar el centro actual si existe ====
        SessionManager sm = SessionManager.getInstance(this);
        if (tvCentroResumen != null) {
            tvCentroResumen.setText(sm.getCentroResumen());
        }

        // ==== Listeners de las tarjetas ====

        // Citas → abre el diálogo para agendar cita
        tileCitas.setOnClickListener(v -> {
            String hoy = getHoy();
            AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newInstance();
            dialog.show(getSupportFragmentManager(), "AgendarCita");
        });

        // Medicamentos → abre pantalla de medicamentos
        tileMedicamentos.setOnClickListener(v ->
                startActivity(new Intent(this, MedicamentosActivity.class))
        );

        // Historial → futuro módulo
        tileHistorial.setOnClickListener(v ->
                Toast.makeText(this, "Historial de citas próximamente", Toast.LENGTH_SHORT).show()
        );

        // Centro de salud → abre mapa de centros
        tileCentro.setOnClickListener(v ->
                startActivity(new Intent(this, CentrosMapaActivity.class))
        );

        // Perfil → abre la ficha del paciente
        tilePerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class))
        );

        // Emergencia → llamada al 112
        tileEmergencia.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
            startActivity(callIntent);
        });

        // ==== Permiso para notificaciones (solo Android 13+) ====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar el centro de salud al volver de otra pantalla
        if (tvCentroResumen != null) {
            tvCentroResumen.setText(SessionManager.getInstance(this).getCentroResumen());
        }
    }

    /**
     * Devuelve la fecha actual formateada como dd/MM/yyyy.
     */
    private String getHoy() {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_MONTH);
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", d, m, y);
    }
}
