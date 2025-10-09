package com.francesco.citapluus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView textViewTitulo;
    private Button buttonAgendarCita, buttonVerCentros, buttonMedicamentos, buttonPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_paciente);

        // 1. Vincular el TextView del título
        textViewTitulo = findViewById(R.id.textViewTitulo);

        // 2. Obtener el nombre del usuario desde el Intent
        String nombreUsuario = getIntent().getStringExtra("usuario");
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            textViewTitulo.setText("Bienvenido, " + nombreUsuario);
        } else {
            textViewTitulo.setText("Bienvenido, Paciente");
        }

        // 3. Vincular los botones
        buttonAgendarCita = findViewById(R.id.buttonAgendarCita);
        buttonVerCentros = findViewById(R.id.buttonVerCentros);
        buttonMedicamentos = findViewById(R.id.buttonMedicamentos);
        buttonPerfil = findViewById(R.id.buttonPerfil);

        // 4. Configurar el botón "Mi Perfil"
        buttonPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PerfilActivity.class);
            startActivity(intent);
        });

        buttonMedicamentos.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MedicamentosActivity.class));
        });


        // 5. Configurar botón "Agendar Cita"
        buttonAgendarCita.setOnClickListener(v -> {
            String hoy = getHoy(); // dd/MM/yyyy
            AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newInstance(hoy);
            dialog.show(getSupportFragmentManager(), "AgendarCita");
        });

        // 6. Otros botones
        buttonVerCentros.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CentrosMapaActivity.class));
        });

        buttonMedicamentos.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MedicamentosActivity.class));
        });

        // 7. Pedir permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    // ✅ Este método debe estar fuera de onCreate()
    private String getHoy() {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_MONTH);
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", d, m, y);
    }
}
