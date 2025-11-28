package com.francesco.citapluus;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.francesco.citapluus.ui.Cita;
import com.francesco.citapluus.data.AppointmentFirestoreRepository;

public class CitaManager {

    private static CitaManager instance = null;
    private final List<Cita> citas = new ArrayList<>();
    private final AppointmentFirestoreRepository appointmentRepo;

    public static synchronized CitaManager getInstance() {
        if (instance == null) instance = new CitaManager();
        return instance;
    }

    private CitaManager() {
        this.appointmentRepo = new AppointmentFirestoreRepository();
    }

    // AGREGAR CITA (guarda en Firestore y localmente)
    public void agregarCita(Cita cita) {
        citas.add(cita);
        
        // Guardar también en Firestore
        appointmentRepo.saveAppointment(cita, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Guardar el ID del documento de Firestore en la cita local
                String firestoreId = task.getResult().getId();
                cita.setFirestoreId(firestoreId);
            }
        });
    }

    // REVISAR SI EXISTE CITA EN FECHA+HORA
    public boolean existeCita(String fecha, String hora) {
        for (Cita c : citas) {
            if (c.getFecha().equals(fecha) && c.getHora().equals(hora)) {
                return true;
            }
        }
        return false;
    }

    // CANCELAR CITA
    public void cancelarCita(long id) {
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getId() == id) {
                Cita citaAEliminar = citas.get(i);
                citas.remove(i);
                
                // Eliminar también de Firestore
                if (citaAEliminar.getFirestoreId() != null) {
                    appointmentRepo.deleteAppointment(citaAEliminar.getFirestoreId(), task -> {
                        // Eliminado de Firestore
                    });
                }
                return;
            }
        }
    }

    // ACTUALIZAR CITA (NECESARIO PARA EDITAR)
    public void actualizarCita(Cita citaActualizada) {
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getId() == citaActualizada.getId()) {
                citas.set(i, citaActualizada);
                
                // Sincronizar con Firestore si tiene firestoreId
                if (citaActualizada.getFirestoreId() != null && !citaActualizada.getFirestoreId().isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fecha", citaActualizada.getFecha());
                    updates.put("hora", citaActualizada.getHora());
                    updates.put("doctorNombre", citaActualizada.getDoctorNombre());
                    updates.put("motivo", citaActualizada.getMotivo());
                    updates.put("estado", citaActualizada.getEstado());
                    
                    // Actualizar centro si existe
                    if (citaActualizada.getCentroNombre() != null && !citaActualizada.getCentroNombre().isEmpty()) {
                        updates.put("centroNombre", citaActualizada.getCentroNombre());
                        updates.put("centroDireccion", citaActualizada.getCentroDireccion());
                        updates.put("centroLat", citaActualizada.getCentroLat());
                        updates.put("centroLng", citaActualizada.getCentroLng());
                    }
                    
                    appointmentRepo.updateAppointment(citaActualizada.getFirestoreId(), updates, task -> {
                        if (task.isSuccessful()) {
                            Log.d("CitaManager", "Cita actualizada en Firestore");
                        } else {
                            Log.e("CitaManager", "Error al actualizar cita en Firestore", task.getException());
                        }
                    });
                }
                return;
            }
        }
    }

    // LISTA DE CITAS
    public List<Cita> getCitas() {
        return citas;
    }
    
    // CARGAR CITAS DESDE FIRESTORE
    public void cargarCitasDesdeFirestore(String dniPaciente, Runnable onComplete) {
        appointmentRepo.getAppointmentsByPatient(dniPaciente, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Cita> citasFirestore = appointmentRepo.documentsToCitas(task.getResult());
                
                // Ordenar las citas por fecha y hora
                Collections.sort(citasFirestore, new Comparator<Cita>() {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    
                    @Override
                    public int compare(Cita c1, Cita c2) {
                        try {
                            String datetime1 = c1.getFecha() + " " + c1.getHora();
                            String datetime2 = c2.getFecha() + " " + c2.getHora();
                            Date d1 = sdf.parse(datetime1);
                            Date d2 = sdf.parse(datetime2);
                            return d1.compareTo(d2);
                        } catch (ParseException e) {
                            return 0;
                        }
                    }
                });
                
                citas.clear();
                citas.addAll(citasFirestore);
            }
            // Ejecutar callback siempre (éxito o fallo)
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
