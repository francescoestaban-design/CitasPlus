package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.francesco.citapluus.data.ProfileRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerfilActivity extends AppCompatActivity {

    private static final int REQUEST_CALL = 1;

    // Inputs
    private EditText editTextCIPA, editTextNombreCompleto, editTextTipoSangre,
            editTextCodigoPostal, editTextAlergias;
    private TextInputLayout tilTipoSangre, tilAlergias, tilCP; // opcionales si pusiste ids
    private android.widget.TextView txtSaveState; // opcional

    private CalendarView calendarView;
    private Button buttonLlamarEmergencia, buttonActualizarCentro, buttonCerrarSesion, buttonVolverMenu;

    // Autosave
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingTipo, pendingAler, pendingCP;
    private static final long AUTOSAVE_DELAY_MS = 400;
    private final Handler saveUi = new Handler(Looper.getMainLooper());
    private Runnable clearMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Bind
        editTextCIPA           = findViewById(R.id.editTextCIPA);
        editTextNombreCompleto = findViewById(R.id.editTextNombreCompleto);
        editTextTipoSangre     = findViewById(R.id.editTextTipoSangre);
        editTextAlergias       = findViewById(R.id.editTextAlergias);
        editTextCodigoPostal   = findViewById(R.id.editTextCodigoPostal);
        calendarView           = findViewById(R.id.calendarView);
        buttonLlamarEmergencia = findViewById(R.id.buttonLlamarEmergencia);
        buttonActualizarCentro = findViewById(R.id.buttonActualizarCentro);
        buttonCerrarSesion     = findViewById(R.id.buttonCerrarSesion);
        buttonVolverMenu       = findViewById(R.id.buttonVolverMenu);

        tilTipoSangre = findViewById(R.id.tilTipoSangre);
        tilAlergias   = findViewById(R.id.tilAlergias);
        tilCP         = findViewById(R.id.tilcp);
        txtSaveState  = findViewById(R.id.txtSaveState);

        // Input constraints
        if (editTextCodigoPostal != null) {
            editTextCodigoPostal.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            editTextCodigoPostal.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(5) });
        }
        if (editTextAlergias != null) {
            editTextAlergias.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(240) });
        }

        cargarDatosUsuario();
        configurarAutosave();
        renderIndicadoresCitas();

        calendarView.setOnDateChangeListener((view, y, m, d) -> {
            String fecha = String.format("%02d/%02d/%04d", d, (m + 1), y);
            SessionManager sm = new SessionManager(this);
            String dni = sm.getDNI();
            List<Cita> citas = CitaManager.getInstance().obtenerCitasPorPaciente(dni);
            Cita encontrada = null;
            if (citas != null) for (Cita c : citas) {
                if (c != null && fecha.equals(c.getFecha())) { encontrada = c; break; }
            }
            if (encontrada != null) {
                AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(encontrada);
                dialog.show(getSupportFragmentManager(), "EditarCita");
            } else {
                Toast.makeText(this, "No tienes cita en esta fecha", Toast.LENGTH_SHORT).show();
            }
        });

        buttonLlamarEmergencia.setOnClickListener(v -> llamarEmergencia());
        buttonActualizarCentro.setOnClickListener(v -> startActivity(new Intent(this, CentrosMapaActivity.class)));
        buttonVolverMenu.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        buttonCerrarSesion.setOnClickListener(v -> {
            new SessionManager(this).logout();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    // ---------- Autosave + validación ----------
    private void configurarAutosave() {
        final SessionManager sm = new SessionManager(this);
        final ProfileRepository repo = ProfileRepository.get(this);

        // Tipo de sangre
        if (editTextTipoSangre != null && editTextTipoSangre.isEnabled()) {
            editTextTipoSangre.addTextChangedListener(new SimpleWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    if (pendingTipo != null) handler.removeCallbacks(pendingTipo);
                    final String raw = s != null ? s.toString() : "";
                    pendingTipo = () -> {
                        String val = sanitizeBlood(raw);
                        if (!isValidBlood(val)) {
                            setError(tilTipoSangre, editTextTipoSangre, "Formato inválido. Ej.: A+, O-, AB+");
                            return;
                        }
                        clearError(tilTipoSangre, editTextTipoSangre);
                        sm.setTipoSangre(val);
                        repo.patchField("tipoSangre", val); // si 404, se desactiva remoto y no spamea
                        savedMsg();
                    };
                    handler.postDelayed(pendingTipo, AUTOSAVE_DELAY_MS);
                }
            });
            editTextTipoSangre.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && pendingTipo != null) { handler.removeCallbacks(pendingTipo); pendingTipo.run(); }
            });
        }

        // Alergias (opcional)
        if (editTextAlergias != null && editTextAlergias.isEnabled()) {
            editTextAlergias.addTextChangedListener(new SimpleWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    if (pendingAler != null) handler.removeCallbacks(pendingAler);
                    final String val = s != null ? s.toString() : "";
                    pendingAler = () -> {
                        clearError(tilAlergias, editTextAlergias);
                        sm.setAlergias(val);
                        repo.patchField("alergias", val);
                        savedMsg();
                    };
                    handler.postDelayed(pendingAler, AUTOSAVE_DELAY_MS);
                }
            });
            editTextAlergias.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && pendingAler != null) { handler.removeCallbacks(pendingAler); pendingAler.run(); }
            });
        }

        // Código postal
        if (editTextCodigoPostal != null && editTextCodigoPostal.isEnabled()) {
            editTextCodigoPostal.addTextChangedListener(new SimpleWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    if (pendingCP != null) handler.removeCallbacks(pendingCP);
                    final String raw = s != null ? s.toString() : "";
                    pendingCP = () -> {
                        String cp = raw.trim();
                        if (!cp.matches("^\\d{5}$")) {
                            setError(tilCP, editTextCodigoPostal, "Debe tener 5 dígitos");
                            return;
                        }
                        clearError(tilCP, editTextCodigoPostal);
                        sm.setCodigoPostal(cp);
                        repo.patchField("codigoPostal", cp);
                        savedMsg();
                    };
                    handler.postDelayed(pendingCP, AUTOSAVE_DELAY_MS);
                }
            });
            editTextCodigoPostal.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && pendingCP != null) { handler.removeCallbacks(pendingCP); pendingCP.run(); }
            });
        }
    }

    private void setError(TextInputLayout til, EditText et, String msg) {
        if (til != null) { til.setError(msg); til.setErrorEnabled(true); }
        else if (et != null) et.setError(msg);
    }
    private void clearError(TextInputLayout til, EditText et) {
        if (til != null) { til.setError(null); til.setErrorEnabled(false); }
        else if (et != null) et.setError(null);
    }

    private String sanitizeBlood(String s) {
        if (s == null) return "";
        String t = s.replace(" ", "").replace("−", "-").replace("—", "-");
        return t.toUpperCase();
    }
    private boolean isValidBlood(String s) {
        if (TextUtils.isEmpty(s)) return false;
        return s.matches("^(A|B|AB|O)[+-]$");
    }

    private void savedMsg() {
        if (txtSaveState == null) return;
        txtSaveState.setText("Guardado");
        if (clearMsg != null) saveUi.removeCallbacks(clearMsg);
        clearMsg = () -> txtSaveState.setText("");
        saveUi.postDelayed(clearMsg, 1200);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public abstract void afterTextChanged(Editable s);
    }

    // ---------- Carga inicial ----------
    private void cargarDatosUsuario() {
        SessionManager sm = new SessionManager(this);
        if (editTextCIPA != null)             editTextCIPA.setText(sm.getCIPA());
        if (editTextNombreCompleto != null)   editTextNombreCompleto.setText(sm.getNombreCompleto());
        if (editTextTipoSangre != null)       editTextTipoSangre.setText(sm.getTipoSangre());
        if (editTextAlergias != null)         editTextAlergias.setText(sm.getAlergias());
        if (editTextCodigoPostal != null)     editTextCodigoPostal.setText(sm.getCodigoPostal());
    }

    // ---------- Citas (igual que ya tenías) ----------
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
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (8 * dp);
                layoutIndicadores.addView(chipBtn, lp);
            }
        }
    }

    // ---------- Emergencias ----------
    private void llamarEmergencia() {
        boolean hasTelephony = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!hasTelephony) {
            startActivity(new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:112")));
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        } else {
            try { startActivity(new Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:112"))); }
            catch (Exception e) { Toast.makeText(this, "Error al realizar la llamada", Toast.LENGTH_SHORT).show(); }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) llamarEmergencia();
            else Toast.makeText(this, "Permiso denegado para llamadas", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
