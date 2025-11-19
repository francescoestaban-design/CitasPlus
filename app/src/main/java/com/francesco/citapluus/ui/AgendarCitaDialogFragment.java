package com.francesco.citapluus.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.francesco.citapluus.ui.Cita;
import com.francesco.citapluus.CitaManager;
import com.francesco.citapluus.R;

import java.util.Calendar;

public class AgendarCitaDialogFragment extends DialogFragment {

    private TextView textViewFecha;
    private Spinner spinnerHora, spinnerDoctor, spinnerMotivo;

    private String fechaSeleccionada = "";
    private boolean esEditar = false;
    private Cita citaAEditar = null;

    // Listener para refrescar pantalla
    public interface OnCitaCreadaListener {
        void onCitaCreada();
    }

    private OnCitaCreadaListener listener;

    public void setListener(OnCitaCreadaListener l) {
        listener = l;
    }

    // === CREAR CITA ===
    public static AgendarCitaDialogFragment newInstance() {
        return new AgendarCitaDialogFragment();
    }

    // === EDITAR CITA ===
    public static AgendarCitaDialogFragment newEditInstance(Cita cita) {
        AgendarCitaDialogFragment frag = new AgendarCitaDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable("edit", cita);
        frag.setArguments(b);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_agendar_cita, container, false);

        textViewFecha = view.findViewById(R.id.textViewFechaSeleccionada);
        spinnerHora = view.findViewById(R.id.spinnerHora);
        spinnerDoctor = view.findViewById(R.id.spinnerDoctor);
        spinnerMotivo = view.findViewById(R.id.spinnerMotivo);

        Button btnConfirmar = view.findViewById(R.id.buttonConfirmarCita);

        setUpSpinners();

        textViewFecha.setOnClickListener(v -> abrirSelectorFecha());

        // Detectar edición
        if (getArguments() != null && getArguments().containsKey("edit")) {
            citaAEditar = (Cita) getArguments().getSerializable("edit");
            esEditar = true;
            cargarDatosEdicion();
        }

        btnConfirmar.setOnClickListener(v -> guardarCita());

        return view;
    }

    private void setUpSpinners() {

        String[] horas = {
                "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "16:00", "17:00"
        };

        spinnerHora.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, horas));

        String[] doctores = {"Dr. Pérez", "Dra. Gómez", "Dr. Martínez"};
        spinnerDoctor.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, doctores));

        String[] motivos = {"Consulta general", "Dolor", "Revisión", "Control"};
        spinnerMotivo.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, motivos));
    }

    private void cargarDatosEdicion() {
        fechaSeleccionada = citaAEditar.getFecha();
        textViewFecha.setText(fechaSeleccionada);

        setSpinnerValue(spinnerHora, citaAEditar.getHora());
        setSpinnerValue(spinnerDoctor, citaAEditar.getDoctorNombre());
        setSpinnerValue(spinnerMotivo, citaAEditar.getMotivo());
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int pos = adapter.getPosition(value);
        if (pos >= 0) spinner.setSelection(pos);
    }

    private void abrirSelectorFecha() {
        final Calendar c = Calendar.getInstance();

        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            fechaSeleccionada = String.format("%02d/%02d/%04d", day, month + 1, year);
            textViewFecha.setText(fechaSeleccionada);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void guardarCita() {

        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(getContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        String hora = spinnerHora.getSelectedItem().toString();
        String doctor = spinnerDoctor.getSelectedItem().toString();
        String motivo = spinnerMotivo.getSelectedItem().toString();

        // === MODO EDITAR ===
        if (esEditar) {
            citaAEditar.setFecha(fechaSeleccionada);
            citaAEditar.setHora(hora);
            citaAEditar.setDoctorNombre(doctor);
            citaAEditar.setMotivo(motivo);

            CitaManager.getInstance().actualizarCita(citaAEditar);
            Toast.makeText(getContext(), "Cita actualizada", Toast.LENGTH_SHORT).show();

            if (listener != null) listener.onCitaCreada();
            dismiss();
            return;
        }

        // === MODO CREAR ===
        long id = System.currentTimeMillis();
        String dniPaciente = SessionManager.getInstance(getContext()).getDNI();

        Cita nueva = new Cita(id, dniPaciente, fechaSeleccionada, hora, motivo, doctor);

        CitaManager.getInstance().agregarCita(nueva);

        Toast.makeText(getContext(), "Cita registrada", Toast.LENGTH_SHORT).show();

        if (listener != null) listener.onCitaCreada();
        dismiss();
    }
}
