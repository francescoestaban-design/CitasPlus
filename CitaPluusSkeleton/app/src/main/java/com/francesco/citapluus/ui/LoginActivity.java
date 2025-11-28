package com.francesco.citapluus.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.francesco.citapluus.LoginDialogFragment;
import com.francesco.citapluus.R;
import com.francesco.citapluus.RegistroDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Pantalla de bienvenida de la aplicación.
 * Si el usuario ya está logueado, redirige automáticamente a MainActivity.
 * Si no está logueado, muestra botones para iniciar sesión o registrarse.
 */
public class LoginActivity extends AppCompatActivity {

    private Button buttonLogin, buttonRegister;
    private FloatingActionButton fabSoporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ✅ Verificar si el usuario ya está logueado
        SessionManager sm = SessionManager.getInstance(this);
        if (sm.isLoggedIn()) {
            // Ya está logueado, ir directamente a MainActivity
            irAMainActivity();
            return;
        }
        
        // Usuario no logueado, mostrar pantalla de bienvenida
        setContentView(R.layout.activity_main);

        // Vincular elementos del XML
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        fabSoporte = findViewById(R.id.fabSoporte);

        // INICIAR SESIÓN → abre el diálogo de login
        buttonLogin.setOnClickListener(v -> {
            LoginDialogFragment loginDialog = new LoginDialogFragment();
            loginDialog.show(getSupportFragmentManager(), "LoginDialog");
        });

        // REGISTRARSE → abre el diálogo de registro
        buttonRegister.setOnClickListener(v -> {
            RegistroDialogFragment registroDialog = new RegistroDialogFragment();
            registroDialog.show(getSupportFragmentManager(), "RegistroDialog");
        });

        // FAB Soporte
        fabSoporte.setOnClickListener(v ->
                Toast.makeText(this, "Soporte: contactanos@citapluus.com", Toast.LENGTH_SHORT).show()
        );
    }
    
    /**
     * Navega a la pantalla principal y cierra esta actividad.
     */
    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
