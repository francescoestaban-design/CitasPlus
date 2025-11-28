package com.francesco.citapluus.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.francesco.citapluus.R;

/**
 * Pantalla de bienvenida que muestra el logo de CitaPlus
 * durante 2 segundos antes de ir a la pantalla principal.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ocultar ActionBar si existe
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Esperar 2 segundos y luego navegar
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Verificar si el usuario ya está logueado
                SessionManager sm = SessionManager.getInstance(SplashActivity.this);
                Intent intent;
                
                if (sm.isLoggedIn()) {
                    // Si está logueado, ir directo a MainActivity
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // Si no está logueado, ir a LoginActivity
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Desactivar el botón de retroceso en el splash
    }
}

