package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private static final int REQUEST_CALL = 1;

    private EditText editTextCIPA, editTextNombreCompleto, editTextTipoSangre, editTextCodigoPostal;
    private CalendarView calendarView;
    private Button buttonLlamarEmergencia, buttonActualizarCentro, buttonCerrarSesion, buttonVolverMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        editTextCIPA           = findViewById(R.id.editTextCIPA);
        editTextNombreCompleto = findViewById(R.id.editTextNombreCompleto);
        editTextTipoSangre     = findViewById(R.id.editTextTipoSangre);
        editTextCodigoPostal   = findViewById(R.id.editTextCodigoPostal);
        calendarView           = findViewById(R.id.calendarView);
        buttonLlamarEmergencia = findViewById(R.id.buttonLlamarEmergencia);
        buttonActualizarCentro = findViewById(R.id.buttonActualizarCentro);
        buttonCerrarSesion     = findViewById(R.id.buttonCerrarSesion);
        buttonVolverMenu       = findViewById(R.id.buttonVolverMenu);

        cargarDatosUsuario();
        renderIndicadoresCitas();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
            SessionManager sm = new SessionManager(PerfilActivity.this);
            String dniPaciente = sm.getDNI();
            List<Cita> citas = CitaManager.getInstance().obtenerCitasPorPaciente(dniPaciente);

            Cita encontrada = null;
            if (citas != null) {
                for (Cita c : citas) {
                    if (c != null && fechaSeleccionada.equals(c.getFecha())) {
                        encontrada = c; break;
                    }
                }
            }
            if (encontrada != null) {
                AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(encontrada);
                dialog.show(getSupportFragmentManager(), "EditarCita");
            } else {
                Toast.makeText(PerfilActivity.this, "No tienes cita en esta fecha", Toast.LENGTH_SHORT).show();
            }
        });

        buttonLlamarEmergencia.setOnClickListener(v -> llamarEmergencia());

        buttonActualizarCentro.setOnClickListener(v ->
                startActivity(new Intent(PerfilActivity.this, CentrosMapaActivity.class))
        );

        buttonVolverMenu.setOnClickListener(v -> {
            startActivity(new Intent(PerfilActivity.this, MainActivity.class));
            finish();
        });

        buttonCerrarSesion.setOnClickListener(v -> {
            SessionManager sessionManager = new SessionManager(PerfilActivity.this);
            sessionManager.logout();
            Toast.makeText(PerfilActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Por si el usuario acaba de guardar un centro desde el mapa y quieres mostrarlo en algún TextView:
        // Ejemplo: Toast con resumen
        SessionManager sm = new SessionManager(this);
        String resumen = sm.getCentroResumen();
        if (!resumen.isEmpty()) {
            // Aquí podrías setText() en un TextView si lo tuvieras. Para demo:
            // Toast.makeText(this, "Centro asignado: " + resumen, Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarDatosUsuario() {
        SessionManager sm = new SessionManager(this);
        editTextCIPA.setText(sm.getCIPA());
        editTextNombreCompleto.setText(sm.getNombreCompleto());
        editTextTipoSangre.setText(sm.getTipoSangre());
        editTextCodigoPostal.setText(sm.getCodigoPostal());
    }

    private void renderIndicadoresCitas() {
        SessionManager sm = new SessionManager(this);
        String dni = sm.getDNI();
        List<Cita> citas = CitaManager.getInstance().obtenerCitasPorPaciente(dni);
        if (citas == null || citas.isEmpty()) return;

        Map<String, List<Cita>> porFecha = new LinkedHashMap<>();
        for (Cita c : citas) {
            if (c == null || c.getFecha() == null) continue;
            porFecha.computeIfAbsent(c.getFecha(), k -> new ArrayList<>()).add(c);
        }

        ChipGroup chipGroup = findViewById(R.id.chipsCitas);
        if (chipGroup != null) {
            chipGroup.removeAllViews();
            for (Map.Entry<String, List<Cita>> e : porFecha.entrySet()) {
                String fecha = e.getKey();
                List<Cita> delDia = e.getValue();

                Chip chip = new Chip(this, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
                chip.setCheckable(false);
                chip.setText(fecha + " • " + delDia.size() + (delDia.size() == 1 ? " cita" : " citas"));
                chip.setChipIconResource(android.R.drawable.ic_menu_today);
                chip.setCloseIconVisible(false);
                chip.setOnClickListener(v -> {
                    try {
                        String[] p = fecha.split("/");
                        int d = Integer.parseInt(p[0]);
                        int m0 = Integer.parseInt(p[1]) - 1;
                        int y = Integer.parseInt(p[2]);
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m0, d, 0, 0, 0);
                        calendarView.setDate(cal.getTimeInMillis(), true, true);

                        Cita primera = delDia.get(0);
                        if (primera != null) {
                            AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(primera);
                            dialog.show(getSupportFragmentManager(), "EditarCita");
                        }
                    } catch (Exception ignore) {}
                });
                chipGroup.addView(chip);
            }
            return;
        }

        LinearLayout layoutIndicadores = findViewById(R.id.layoutIndicadores);
        if (layoutIndicadores != null) {
            layoutIndicadores.removeAllViews();
            float dp = getResources().getDisplayMetrics().density;

            for (Map.Entry<String, List<Cita>> e : porFecha.entrySet()) {
                String fecha = e.getKey();
                List<Cita> delDia = e.getValue();

                Button chipBtn = new Button(this);
                chipBtn.setAllCaps(false);
                chipBtn.setText(fecha + " • " + delDia.size() + (delDia.size() == 1 ? " cita" : " citas"));
                chipBtn.setPadding((int) (16 * dp), (int) (8 * dp), (int) (16 * dp), (int) (8 * dp));
                chipBtn.setOnClickListener(v -> {
                    try {
                        String[] p = fecha.split("/");
                        int d = Integer.parseInt(p[0]);
                        int m0 = Integer.parseInt(p[1]) - 1;
                        int y = Integer.parseInt(p[2]);
                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m0, d, 0, 0, 0);
                        calendarView.setDate(cal.getTimeInMillis(), true, true);

                        Cita primera = delDia.get(0);
                        if (primera != null) {
                            AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(primera);
                            dialog.show(getSupportFragmentManager(), "EditarCita");
                        }
                    } catch (Exception ignore) {}
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (8 * dp);
                layoutIndicadores.addView(chipBtn, lp);
            }
        }
    }

    private void llamarEmergencia() {
        boolean hasTelephony = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!hasTelephony) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")));
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            try {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:112")));
            } catch (Exception e) {
                Toast.makeText(this, "Error al realizar la llamada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                llamarEmergencia();
            } else {
                Toast.makeText(this, "Permiso denegado para llamadas", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
