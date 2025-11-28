package com.francesco.citapluus.ui;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.francesco.citapluus.ui.Cita;
import com.francesco.citapluus.CitaManager;
import com.francesco.citapluus.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvCipa, tvNombre, tvTipoSangre, tvAlergias, tvCodigoPostal;
    private TextView tvProximasCitas;
    private ChipGroup chipsCitas;
    private CalendarView calendarView;
    private LinearLayout layoutIndicadores;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // === BINDINGS ===
        tvCipa = findViewById(R.id.editTextCIPA);
        tvNombre = findViewById(R.id.editTextNombreCompleto);
        tvTipoSangre = findViewById(R.id.editTextTipoSangre);
        tvAlergias = findViewById(R.id.editTextAlergias);
        tvCodigoPostal = findViewById(R.id.editTextCodigoPostal);

        calendarView = findViewById(R.id.calendarView);
        chipsCitas = findViewById(R.id.chipsCitas);
        tvProximasCitas = findViewById(R.id.tvProximasCitas);
        layoutIndicadores = findViewById(R.id.layoutIndicadores);

        cargarDatosUsuario();
        cargarCitas();
    }
    private void agregarTextoCentro(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextSize(15f);
        t.setPadding(8, 8, 8, 8);
        layoutIndicadores.addView(t);
    }

    private void cargarDatosUsuario() {
        SessionManager sm = SessionManager.getInstance(getApplicationContext());

        tvCipa.setText(sm.getCIPA());
        tvNombre.setText(sm.getNombreCompleto());
        tvTipoSangre.setText(sm.getTipoSangre());
        tvAlergias.setText(sm.getAlergias());
        tvCodigoPostal.setText(sm.getCodigoPostal());
    }

    private void cargarCitas() {
        layoutIndicadores.removeAllViews();

        List<Cita> citas = CitaManager.getInstance().getCitas();

        if (citas.isEmpty()) {
            agregarTextoCentro("No tienes próximas citas");
            return;
        }

        for (Cita c : citas) {
            TextView t = new TextView(this);
            t.setText("• " + c.getFecha() + " - " + c.getHora() + " con " + c.getDoctorNombre());
            t.setPadding(0, 12, 0, 12);
            t.setTextSize(15f);
            t.setClickable(true);

            t.setOnClickListener(v -> abrirEditarCita(c));

            layoutIndicadores.addView(t);
        }
    }

    private void abrirEditarCita(Cita cita) {
        AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(cita);
        dialog.show(getSupportFragmentManager(), "editarCita");
    }


    @Override
    protected void onResume() {
        super.onResume();
        cargarCitas();
    }
}
