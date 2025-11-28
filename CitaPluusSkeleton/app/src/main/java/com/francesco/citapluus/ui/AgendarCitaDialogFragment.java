package com.francesco.citapluus.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.francesco.citapluus.ui.Cita;
import com.francesco.citapluus.CitaManager;
import com.francesco.citapluus.R;
import com.francesco.citapluus.data.AppointmentFirestoreRepository;

import java.util.Calendar;

public class AgendarCitaDialogFragment extends DialogFragment {

    private com.google.android.material.textfield.TextInputEditText textViewFecha;
    private TextView textViewCentroAsignado;
    private Spinner spinnerHora, spinnerDoctor;
    private EditText editTextMotivoConsulta;

    private String fechaSeleccionada = "";
    private boolean esEditar = false;
    private Cita citaAEditar = null;
    private boolean pendienteConfirmacion = false;

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

    @Override
    public void onStart() {
        super.onStart();
        // Ajustar el ancho del diálogo para que no se vea tan fino
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_agendar_cita, container, false);

        textViewFecha = view.findViewById(R.id.textViewFechaSeleccionada);
        textViewCentroAsignado = view.findViewById(R.id.textViewCentroAsignado);
        spinnerHora = view.findViewById(R.id.spinnerHora);
        spinnerDoctor = view.findViewById(R.id.spinnerDoctor);
        editTextMotivoConsulta = view.findViewById(R.id.editTextMotivoConsulta);

        Button btnConfirmar = view.findViewById(R.id.buttonConfirmarCita);

        // Mostrar centro asignado
        SessionManager sm = SessionManager.getInstance(getContext());
        String centroNombre = sm.getCentroNombre();
        String centroDireccion = sm.getCentroDireccion();
        
        if (centroNombre != null && !centroNombre.isEmpty()) {
            String infoCompleta = centroNombre;
            if (centroDireccion != null && !centroDireccion.isEmpty()) {
                infoCompleta += "\n" + centroDireccion;
            }
            textViewCentroAsignado.setText(infoCompleta);
        } else {
            textViewCentroAsignado.setText("⚠️ Sin centro asignado (toca para seleccionar)");
            textViewCentroAsignado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            textViewCentroAsignado.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(getContext(), CentrosMapaActivity.class);
                startActivity(intent);
                dismiss();
            });
        }

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

        // ✅ Especialidades médicas comunes en centros de salud
        String[] especialidades = {
                "Medicina General",
                "Pediatría",
                "Enfermería",
                "Ginecología",
                "Traumatología",
                "Cardiología",
                "Dermatología",
                "Oftalmología"
        };
        spinnerDoctor.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, especialidades));
    }

    private void cargarDatosEdicion() {
        fechaSeleccionada = citaAEditar.getFecha();
        textViewFecha.setText(fechaSeleccionada);

        setSpinnerValue(spinnerHora, citaAEditar.getHora());
        setSpinnerValue(spinnerDoctor, citaAEditar.getDoctorNombre());
        editTextMotivoConsulta.setText(citaAEditar.getMotivo());
    }

    @SuppressWarnings("unchecked")
    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
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

        String motivo = editTextMotivoConsulta.getText().toString().trim();
        if (motivo.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa el motivo de la consulta", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el usuario está logueado
        SessionManager sm = SessionManager.getInstance(getContext());
        if (sm.getDNI() == null || sm.getDNI().isEmpty()) {
            Toast.makeText(getContext(), "Debes iniciar sesión para agendar una cita", Toast.LENGTH_LONG).show();
            pendienteConfirmacion = true;
            
            // Mostrar dialog de login con callback para continuar después del login
            com.francesco.citapluus.LoginDialogFragment loginDialog = new com.francesco.citapluus.LoginDialogFragment();
            loginDialog.setOnLoginSuccessListener(new com.francesco.citapluus.LoginDialogFragment.OnLoginSuccessListener() {
                @Override
                public void onLoginSuccess() {
                    // Reintentar guardar la cita ahora que el usuario está logueado
                    guardarCita();
                }
            });
            loginDialog.show(getParentFragmentManager(), "login");
            return;
        }

        // ===== VERIFICAR SI TIENE CENTRO DE SALUD ASIGNADO =====
        if (sm.getCentroNombre() == null || sm.getCentroNombre().isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Centro de salud requerido")
                    .setMessage("Debes seleccionar un centro de salud antes de agendar una cita.\n\n¿Deseas seleccionar un centro ahora?")
                    .setPositiveButton("Sí, seleccionar", (dialog, which) -> {
                        // Abrir actividad de centros
                        android.content.Intent intent = new android.content.Intent(getContext(), CentrosMapaActivity.class);
                        startActivity(intent);
                        dismiss(); // Cerrar el diálogo de agendar cita
                    })
                    .setNegativeButton("Cancelar", null)
                    .setCancelable(false)
                    .show();
            return;
        }

        String hora = spinnerHora.getSelectedItem().toString();
        String doctor = spinnerDoctor.getSelectedItem().toString();

        // === MODO EDITAR ===
        if (esEditar) {
            // Para edición, también verificar disponibilidad si cambia doctor/fecha/hora
            verificarDisponibilidadYGuardar(doctor, fechaSeleccionada, hora, motivo, true);
            return;
        }

        // === MODO CREAR ===
        // Verificar disponibilidad antes de crear
        verificarDisponibilidadYGuardar(doctor, fechaSeleccionada, hora, motivo, false);
    }

    /**
     * Verifica si el horario está disponible antes de guardar la cita
     */
    private void verificarDisponibilidadYGuardar(String doctor, String fecha, String hora, String motivo, boolean modoEditar) {
        AppointmentFirestoreRepository repo = new AppointmentFirestoreRepository();
        
        // Verificar si existe una cita con ese doctor, fecha y hora
        repo.verificarDisponibilidad(doctor, fecha, hora, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int citasExistentes = task.getResult().size();
                
                // Si hay citas existentes en ese horario
                if (citasExistentes > 0) {
                    // En modo editar, verificar si la cita existente es la misma que estamos editando
                    if (modoEditar && citaAEditar != null) {
                        boolean esLaMismaCita = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String dniEnCita = doc.getString("dniPaciente");
                            if (dniEnCita != null && dniEnCita.equals(citaAEditar.getDniPaciente())) {
                                esLaMismaCita = true;
                                break;
                            }
                        }
                        
                        if (esLaMismaCita) {
                            // Es la misma cita, permitir actualización
                            actualizarCitaExistente(motivo, fecha, hora, doctor);
                            return;
                        }
                    }
                    
                    // Horario ocupado por otra persona
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), 
                                "❌ Hora no disponible. El " + doctor + " ya tiene una cita el " + fecha + " a las " + hora + 
                                ". Por favor, selecciona otro horario.", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    // Horario disponible, proceder a guardar
                    if (modoEditar) {
                        actualizarCitaExistente(motivo, fecha, hora, doctor);
                    } else {
                        crearNuevaCita(motivo, fecha, hora, doctor);
                    }
                }
            } else {
                // Error al verificar, pero permitir guardar (modo offline)
                if (modoEditar) {
                    actualizarCitaExistente(motivo, fecha, hora, doctor);
                } else {
                    crearNuevaCita(motivo, fecha, hora, doctor);
                }
            }
        });
    }

    /**
     * Crea una nueva cita
     */
    private void crearNuevaCita(String motivo, String fecha, String hora, String doctor) {
        long id = System.currentTimeMillis();
        SessionManager sm = SessionManager.getInstance(getContext());
        String dniPaciente = sm.getDNI();

        Cita nueva = new Cita(id, dniPaciente, fecha, hora, motivo, doctor);
        
        // Guardar centro de salud asignado en la cita
        nueva.setCentroNombre(sm.getCentroNombre());
        nueva.setCentroDireccion(sm.getCentroDireccion());
        nueva.setCentroLat(sm.getCentroLat());
        nueva.setCentroLng(sm.getCentroLng());
        nueva.setEstado("pendiente");
        
        CitaManager.getInstance().agregarCita(nueva);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "✅ Cita registrada correctamente", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onCitaCreada();
                dismiss();
            });
        }
    }

    /**
     * Actualiza una cita existente
     */
    private void actualizarCitaExistente(String motivo, String fecha, String hora, String doctor) {
        citaAEditar.setFecha(fecha);
        citaAEditar.setHora(hora);
        citaAEditar.setDoctorNombre(doctor);
        citaAEditar.setMotivo(motivo);

        CitaManager.getInstance().actualizarCita(citaAEditar);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "✅ Cita actualizada", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onCitaCreada();
                dismiss();
            });
        }
    }
}
