package com.francesco.citapluus;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import com.francesco.citapluus.SessionManager;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;

public class WorkerMainActivity extends AppCompatActivity {

    private CalendarView calendarViewCitas;
    private Button buttonVerPacientes, buttonSolicitarMedicamentos, buttonCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_main);

        // Referencias
        calendarViewCitas = findViewById(R.id.calendarViewCitas);
        buttonVerPacientes = findViewById(R.id.buttonVerPacientes);
        buttonSolicitarMedicamentos = findViewById(R.id.buttonSolicitarMedicamentos);
        buttonCerrarSesion = findViewById(R.id.buttonCerrarSesionTrabajador);

        // Configurar calendario
        calendarViewCitas.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Toast.makeText(this, "Citas programadas para: " + dayOfMonth + "/" + (month + 1) + "/" + year, Toast.LENGTH_SHORT).show();

        });

        // Botón: Ver Lista de Pacientes
        buttonVerPacientes.setOnClickListener(v -> {
            Toast.makeText(this, "Lista de pacientes (próximamente)", Toast.LENGTH_SHORT).show();
            // Aquí luego abrirás una nueva actividad con RecyclerView de pacientes
        });

        // Botón: Solicitar Medicamentos
        buttonSolicitarMedicamentos.setOnClickListener(v -> {
            Toast.makeText(this, "Formulario de solicitud de medicamentos (próximamente)", Toast.LENGTH_SHORT).show();
        });

        // Botón: Cerrar Sesión
        buttonCerrarSesion.setOnClickListener(v -> {
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.logout();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}