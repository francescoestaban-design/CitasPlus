package com.francesco.citapluus;

import com.francesco.citapluus.data.FavoritesRepository;

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

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView textViewTitulo;
    private Button buttonAgendarCita, buttonVerCentros, buttonMedicamentos, buttonPerfil, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // IMPORTANTE: este layout debe contener btnSettings
        setContentView(R.layout.activity_main_paciente);

        textViewTitulo     = findViewById(R.id.textViewTitulo);
        buttonAgendarCita  = findViewById(R.id.buttonAgendarCita);
        buttonVerCentros   = findViewById(R.id.buttonVerCentros);
        buttonMedicamentos = findViewById(R.id.buttonMedicamentos);
        buttonPerfil       = findViewById(R.id.buttonPerfil);
        btnSettings        = findViewById(R.id.btnSettings);   // <-- nuevo

        // Título
        String nombreUsuario = getIntent().getStringExtra("usuario");
        textViewTitulo.setText(
                (nombreUsuario != null && !nombreUsuario.isEmpty())
                        ? "Bienvenido, " + nombreUsuario
                        : "Bienvenido, Paciente"
        );

        // Navegación
        buttonPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class)));

        buttonMedicamentos.setOnClickListener(v ->
                startActivity(new Intent(this, MedicamentosActivity.class)));

        buttonAgendarCita.setOnClickListener(v -> {
            String hoy = getHoy();
            AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newInstance(hoy);
            dialog.show(getSupportFragmentManager(), "AgendarCita");
        });

        buttonVerCentros.setOnClickListener(v ->
                startActivity(new Intent(this, CentrosMapaActivity.class)));

        // Ajustes (abre SettingsActivity)
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }

        findViewById(R.id.btnProbarApi).setOnClickListener(v -> {
            com.francesco.citapluus.net.core.ApiService api =
                    com.francesco.citapluus.net.core.RetrofitProvider
                            .get(this).create(com.francesco.citapluus.net.core.ApiService.class);

            api.ping().enqueue(new retrofit2.Callback<com.francesco.citapluus.net.core.ApiService.PingResp>() {
                @Override public void onResponse(retrofit2.Call<com.francesco.citapluus.net.core.ApiService.PingResp> call,
                                                 retrofit2.Response<com.francesco.citapluus.net.core.ApiService.PingResp> resp) {
                    String msg = (resp.body() != null) ? resp.body().msg : ("HTTP " + resp.code());
                    android.widget.Toast.makeText(MainActivity.this, "PING: " + msg, android.widget.Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(retrofit2.Call<com.francesco.citapluus.net.core.ApiService.PingResp> call,
                                                Throwable t) {
                    android.widget.Toast.makeText(MainActivity.this, "PING error: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ===== DEMO rápido Favoritos Mock =====
        findViewById(R.id.btnProbarApi).post(() -> {
            FavoritesRepository repo = new FavoritesRepository(this);

            // 1) GET
            repo.fetch(new FavoritesRepository.RepoCallback<java.util.List<com.francesco.citapluus.FavoritePlace>>() {
                @Override public void onSuccess(java.util.List<com.francesco.citapluus.FavoritePlace> data) {
                    android.util.Log.i("NetworkCfg", "GET /favorites -> size=" + data.size());
                }
                @Override public void onError(Throwable error) {
                    android.util.Log.e("NetworkCfg", "GET /favorites error", error);
                }
            });

            // 2) POST (añadir otro)
            com.francesco.citapluus.FavoritePlace nuevo =
                    new com.francesco.citapluus.FavoritePlace(
                            "ph_2","Farmacia Norte","Av. Norte 456",40.42,-3.70,"FARMACIA");
            repo.add(nuevo, new FavoritesRepository.RepoCallback<com.francesco.citapluus.FavoritePlace>() {
                @Override public void onSuccess(com.francesco.citapluus.FavoritePlace data) {
                    android.util.Log.i("NetworkCfg", "POST /favorites OK id=" + data.id);
                    // 3) GET para verificar que creció
                    repo.fetch(new FavoritesRepository.RepoCallback<java.util.List<com.francesco.citapluus.FavoritePlace>>() {
                        @Override public void onSuccess(java.util.List<com.francesco.citapluus.FavoritePlace> data) {
                            android.util.Log.i("NetworkCfg", "GET /favorites (after add) -> size=" + data.size());
                        }
                        @Override public void onError(Throwable error) {}
                    });
                    // 4) DELETE para probar borrado
                    repo.remove("ph_2", new FavoritesRepository.RepoCallback<Void>() {
                        @Override public void onSuccess(Void v) {
                            android.util.Log.i("NetworkCfg", "DELETE /favorites/ph_2 OK");
                        }
                        @Override public void onError(Throwable error) {
                            android.util.Log.e("NetworkCfg", "DELETE error", error);
                        }
                    });
                }
                @Override public void onError(Throwable error) {
                    android.util.Log.e("NetworkCfg", "POST /favorites error", error);
                }
            });
        });




        // Permiso notificaciones Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100
            );
        }
    }


    private String getHoy() {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_MONTH);
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", d, m, y);
    }
}
