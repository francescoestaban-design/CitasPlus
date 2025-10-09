package com.francesco.citapluus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class LoginActivity extends AppCompatActivity {

    private Button buttonLogin, buttonRegister;
    private FloatingActionButton fabSoporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Usamos el layout activity_main con los IDs que definimos abajo
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
                Toast.makeText(this, "Chat de soporte (próximamente)", Toast.LENGTH_SHORT).show()
        );
    }
}
