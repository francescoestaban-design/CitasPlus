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
import com.francesco.citapluus.ui.SessionManager;
import com.francesco.citapluus.data.UserRepository;
import com.francesco.citapluus.data.AppointmentFirestoreRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvCipa, tvNombre;
    private com.google.android.material.textfield.TextInputEditText tvTipoSangre, tvAlergias, tvCodigoPostal;
    private TextView tvProximasCitas;
    private ChipGroup chipsCitas;
    private CalendarView calendarView;
    private LinearLayout layoutIndicadores;
    private com.google.android.material.button.MaterialButton buttonGuardarCambios;

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
        buttonGuardarCambios = findViewById(R.id.buttonGuardarCambios);

        // Botón guardar cambios
        buttonGuardarCambios.setOnClickListener(v -> guardarCambios());

        // Botón llamar emergencia
        findViewById(R.id.buttonLlamarEmergencia).setOnClickListener(v -> {
            android.content.Intent callIntent = new android.content.Intent(android.content.Intent.ACTION_DIAL, 
                    android.net.Uri.parse("tel:112"));
            startActivity(callIntent);
        });

        // Botón cambiar centro
        findViewById(R.id.buttonActualizarCentro).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, CentrosMapaActivity.class);
            startActivity(intent);
        });

        // Botón cerrar sesión
        findViewById(R.id.buttonCerrarSesion).setOnClickListener(v -> {
            SessionManager.getInstance(getApplicationContext()).logout();
            android.widget.Toast.makeText(this, "Sesión cerrada", android.widget.Toast.LENGTH_SHORT).show();
            
            // ✅ Volver a la pantalla de login y limpiar el historial de actividades
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Botón volver al menú principal
        findViewById(R.id.buttonVolverMenu).setOnClickListener(v -> finish());

        cargarDatosUsuario();
        
        // Cargar citas desde Firestore primero, luego mostrar (EN HILO DE FONDO)
        SessionManager sm = SessionManager.getInstance(getApplicationContext());
        if (sm.isLoggedIn() && !sm.getDNI().isEmpty()) {
            new Thread(() -> {
                CitaManager.getInstance().cargarCitasDesdeFirestore(sm.getDNI(), () -> {
                    // Una vez cargadas desde Firestore, mostrar en la UI (en el hilo principal)
                    runOnUiThread(() -> {
                        cargarCitas();
                        android.util.Log.d("PerfilActivity", "Citas cargadas al iniciar: " + CitaManager.getInstance().getCitas().size());
                    });
                });
            }).start();
        } else {
            cargarCitas();
        }

        // Configurar listener para clics en días del calendario
        configurarListenerCalendario();
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
        android.util.Log.d("PerfilActivity", "cargarCitas() llamado");
        
        layoutIndicadores.removeAllViews();
        chipsCitas.removeAllViews();

        List<Cita> citas = CitaManager.getInstance().getCitas();
        
        android.util.Log.d("PerfilActivity", "Número de citas: " + citas.size());

        if (citas.isEmpty()) {
            agregarTextoCentro("No tienes próximas citas");
            android.util.Log.d("PerfilActivity", "No hay citas, mostrando mensaje");
            return;
        }

        // Ocultar el TextView "No tienes próximas citas"
        tvProximasCitas.setVisibility(android.view.View.GONE);

        for (Cita c : citas) {
            android.util.Log.d("PerfilActivity", "Procesando cita: " + c.getFecha() + " " + c.getHora());
            
            // Crear Chip para marcar el día con cita (chips rojos debajo del calendario)
            Chip chipFecha = new Chip(this);
            chipFecha.setText(c.getFecha());
            chipFecha.setChipBackgroundColorResource(android.R.color.holo_red_light);
            chipFecha.setTextColor(getResources().getColor(android.R.color.white));
            chipFecha.setCloseIconVisible(false);
            chipFecha.setClickable(true);
            chipFecha.setOnClickListener(v -> mostrarDialogoEditarCita(c));
            chipsCitas.addView(chipFecha);
            
            // Crear TextView para mostrar la cita en la lista
            TextView t = new TextView(this);
            String textoCita = "• " + c.getFecha() + " - " + c.getHora() + " con " + c.getDoctorNombre();
            t.setText(textoCita);
            t.setPadding(0, 16, 0, 16);
            t.setTextSize(16f);
            t.setClickable(true);
            t.setTextColor(getResources().getColor(android.R.color.black));
            t.setBackground(getResources().getDrawable(android.R.drawable.list_selector_background));

            // Click listener para editar/cancelar cita
            t.setOnClickListener(v -> mostrarDialogoEditarCita(c));

            layoutIndicadores.addView(t);

            // Agregar línea divisoria
            android.view.View divider = new android.view.View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    2
            ));
            divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            layoutIndicadores.addView(divider);
        }

        // Actualizar listener del calendario con las nuevas citas
        configurarListenerCalendario();
        
        android.util.Log.d("PerfilActivity", "Citas mostradas correctamente");
    }

    /**
     * Configura el listener del calendario para detectar clics en días con citas
     */
    private void configurarListenerCalendario() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Verificar si este día tiene citas
            for (Cita c : CitaManager.getInstance().getCitas()) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                    Date citaDate = sdf.parse(c.getFecha());
                    
                    java.util.Calendar citaCal = java.util.Calendar.getInstance();
                    citaCal.setTime(citaDate);

                    if (citaCal.get(java.util.Calendar.YEAR) == year &&
                        citaCal.get(java.util.Calendar.MONTH) == month &&
                        citaCal.get(java.util.Calendar.DAY_OF_MONTH) == dayOfMonth) {
                        // Es un día con cita - mostrar diálogo
                        mostrarDialogoEditarCita(c);
                        return;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            
            // No hay cita en este día
            android.widget.Toast.makeText(this, "No tienes citas en este día", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Muestra un diálogo para editar o cancelar una cita
     */
    private void mostrarDialogoEditarCita(Cita cita) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        android.view.View dialogView = android.view.LayoutInflater.from(this)
                .inflate(R.layout.dialog_agendar_cita, null);

        // Configurar los campos con los datos de la cita (TIPOS CORRECTOS)
        android.widget.TextView textFecha = dialogView.findViewById(R.id.textViewFechaSeleccionada);
        android.widget.Spinner spinnerHora = dialogView.findViewById(R.id.spinnerHora);
        android.widget.Spinner spinnerDoctor = dialogView.findViewById(R.id.spinnerDoctor);
        com.google.android.material.textfield.TextInputEditText editMotivo = dialogView.findViewById(R.id.editTextMotivoConsulta);
        com.google.android.material.button.MaterialButton btnConfirmar = dialogView.findViewById(R.id.buttonConfirmarCita);

        // Llenar los campos
        textFecha.setText(cita.getFecha());
        editMotivo.setText(cita.getMotivo());
        
        // Deshabilitar edición
        textFecha.setEnabled(false);
        spinnerHora.setEnabled(false);
        spinnerDoctor.setEnabled(false);
        editMotivo.setEnabled(false);

        builder.setView(dialogView)
                .setTitle("Detalle de Cita")
                .setCancelable(true);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Cambiar botón "Confirmar" a "Cancelar"
        btnConfirmar.setText("Cancelar cita");
        btnConfirmar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        
        btnConfirmar.setOnClickListener(v -> {
            // Mostrar confirmación de cancelación
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirmar cancelación")
                    .setMessage("¿Estás seguro de que quieres cancelar esta cita?")
                    .setPositiveButton("Sí, cancelar", (dialog1, which) -> {
                        // Cancelar cita
                        cancelarCitaEnFirestore(cita);
                        dialog.dismiss();
                    })
                    .setNegativeButton("No, mantener", null)
                    .show();
        });

        dialog.show();
    }

    /**
     * Cancela una cita en Firestore
     */
    private void cancelarCitaEnFirestore(Cita cita) {
        SessionManager sm = SessionManager.getInstance(getApplicationContext());
        
        // Obtener el DNI del usuario
        String dni = sm.getDNI();
        if (dni == null || dni.isEmpty()) {
            android.widget.Toast.makeText(this, "Error: no se pudo obtener el DNI del usuario", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Eliminar de CitaManager (local) primero
        CitaManager.getInstance().cancelarCita(cita.getId());
        
        // Actualizar vista inmediatamente
        runOnUiThread(() -> cargarCitas());

        // Eliminar de Firestore
        AppointmentFirestoreRepository appointmentRepo = new AppointmentFirestoreRepository();
        
        // Buscar la cita en Firestore por DNI y eliminarla
        appointmentRepo.getAppointmentsByPatient(dni, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Cita citaFirestore = appointmentRepo.documentToCita(doc);
                    if (citaFirestore != null && 
                        citaFirestore.getFecha().equals(cita.getFecha()) &&
                        citaFirestore.getHora().equals(cita.getHora())) {
                        
                        // Eliminar este documento
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    runOnUiThread(() -> {
                                        android.widget.Toast.makeText(this, "Cita cancelada correctamente", android.widget.Toast.LENGTH_SHORT).show();
                                        android.util.Log.d("PerfilActivity", "Cita eliminada de Firestore");
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    runOnUiThread(() -> {
                                        android.widget.Toast.makeText(this, "Error al cancelar la cita en la nube", android.widget.Toast.LENGTH_SHORT).show();
                                        android.util.Log.e("PerfilActivity", "Error al eliminar cita", e);
                                    });
                                });
                        return;
                    }
                }
            } else {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Error al buscar la cita en la nube", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void abrirEditarCita(Cita cita) {
        AgendarCitaDialogFragment dialog = AgendarCitaDialogFragment.newEditInstance(cita);
        dialog.show(getSupportFragmentManager(), "editarCita");
    }

    private void guardarCambios() {
        SessionManager sm = SessionManager.getInstance(getApplicationContext());
        
        // Obtener los valores de los campos editables
        String tipoSangre = tvTipoSangre.getText() != null ? tvTipoSangre.getText().toString().trim() : "";
        String alergias = tvAlergias.getText() != null ? tvAlergias.getText().toString().trim() : "";
        String codigoPostal = tvCodigoPostal.getText() != null ? tvCodigoPostal.getText().toString().trim() : "";
        
        // ✅ Validaciones mejoradas con mensajes específicos
        if (tipoSangre.isEmpty()) {
            android.widget.Toast.makeText(this, "El tipo de sangre es obligatorio", android.widget.Toast.LENGTH_SHORT).show();
            tvTipoSangre.requestFocus();
            return;
        }
        
        if (codigoPostal.isEmpty()) {
            android.widget.Toast.makeText(this, "El código postal es obligatorio", android.widget.Toast.LENGTH_SHORT).show();
            tvCodigoPostal.requestFocus();
            return;
        }
        
        if (codigoPostal.length() != 5) {
            android.widget.Toast.makeText(this, "El código postal debe tener exactamente 5 dígitos", android.widget.Toast.LENGTH_SHORT).show();
            tvCodigoPostal.requestFocus();
            return;
        }

        if (!codigoPostal.matches("[0-9]{5}")) {
            android.widget.Toast.makeText(this, "El código postal solo puede contener números", android.widget.Toast.LENGTH_SHORT).show();
            tvCodigoPostal.requestFocus();
            return;
        }

        if (!alergias.isEmpty() && !alergias.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9, ]+")) {
            android.widget.Toast.makeText(this, "Las alergias solo pueden contener letras, números y comas", android.widget.Toast.LENGTH_SHORT).show();
            tvAlergias.requestFocus();
            return;
        }
        // Guardar los cambios en SessionManager (local)
        sm.setTipoSangre(tipoSangre);
        sm.setAlergias(alergias);
        sm.setCodigoPostal(codigoPostal);
        
        // Preparar los cambios para Firestore
        String dni = sm.getDNI();
        if (dni != null && !dni.isEmpty()) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("tipoSangre", tipoSangre);
            updates.put("alergias", alergias);
            updates.put("codigoPostal", codigoPostal);

            UserRepository userRepo = new UserRepository();
            userRepo.updateUser(dni, updates, task -> {
                if (task.isSuccessful()) {
                    android.widget.Toast.makeText(this, "Cambios guardados correctamente en la nube", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(this, "Cambios guardados localmente (sin conexión a internet)", android.widget.Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Si no hay DNI, solo guardar localmente
            android.widget.Toast.makeText(this, "Cambios guardados localmente", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Recargar citas desde Firestore (EN HILO DE FONDO)
        SessionManager sm = SessionManager.getInstance(getApplicationContext());
        if (sm.isLoggedIn() && !sm.getDNI().isEmpty()) {
            new Thread(() -> {
                CitaManager.getInstance().cargarCitasDesdeFirestore(sm.getDNI(), () -> {
                    // Ejecutar en el hilo principal
                    runOnUiThread(() -> {
                        cargarCitas();
                        android.util.Log.d("PerfilActivity", "Citas recargadas: " + CitaManager.getInstance().getCitas().size());
                    });
                });
            }).start();
        } else {
            cargarCitas();
        }
    }
}
