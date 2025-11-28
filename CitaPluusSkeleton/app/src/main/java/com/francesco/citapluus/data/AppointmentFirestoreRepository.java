package com.francesco.citapluus.data;

import android.util.Log;

import com.francesco.citapluus.ui.Cita;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class AppointmentFirestoreRepository {

    private static final String TAG = "AppointmentRepository";
    private static final String COLLECTION_CITAS = "citas";
    
    private final FirebaseFirestore db;

    public AppointmentFirestoreRepository() {
        this.db = FirebaseService.getInstance().getDb();
    }

    /**
     * Guarda una cita en Firestore
     */
    public void saveAppointment(Cita cita, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("id", cita.getId());
        appointmentData.put("dniPaciente", cita.getDniPaciente());
        appointmentData.put("fecha", cita.getFecha());
        appointmentData.put("hora", cita.getHora());
        appointmentData.put("doctorNombre", cita.getDoctorNombre());
        appointmentData.put("motivo", cita.getMotivo());
        appointmentData.put("estado", cita.getEstado() != null ? cita.getEstado() : "pendiente");
        appointmentData.put("fechaCreacion", com.google.firebase.Timestamp.now());
        
        // Guardar información del centro de salud
        if (cita.getCentroNombre() != null && !cita.getCentroNombre().isEmpty()) {
            appointmentData.put("centroNombre", cita.getCentroNombre());
            appointmentData.put("centroDireccion", cita.getCentroDireccion());
            appointmentData.put("centroLat", cita.getCentroLat());
            appointmentData.put("centroLng", cita.getCentroLng());
        }
        //Registro de citas
        db.collection(COLLECTION_CITAS)
                .add(appointmentData)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar cita", e);
                });
    }

    /**
     * Obtiene todas las citas de un paciente
     * Nota: Dl ordenamiento se hace en el cliente para evitar índices compuestos
     */
    public void getAppointmentsByPatient(String dniPaciente, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_CITAS)
                .whereEqualTo("dniPaciente", dniPaciente)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener citas", e);
                });
    }

    /**
     * Verifica si existe una cita con un doctor, fecha y hora específicos
     * (para validar disponibilidad)
     */
    public void verificarDisponibilidad(String doctorNombre, String fecha, String hora, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_CITAS)
                .whereEqualTo("doctorNombre", doctorNombre)
                .whereEqualTo("fecha", fecha)
                .whereEqualTo("hora", hora)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al verificar disponibilidad", e);
                });
    }

    /**
     * Actualiza una cita
     */
    public void updateAppointment(String appointmentId, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_CITAS)
                .document(appointmentId)
                .update(updates)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar cita", e);
                });
    }

    /**
     * Elimina una cita
     */
    public void deleteAppointment(String appointmentId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_CITAS)
                .document(appointmentId)
                .delete()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar cita", e);
                });
    }

    /**
     * Elimina todas las citas de un paciente
     */
    public void deleteAllAppointmentsByPatient(String dniPaciente, OnCompleteListener<Void> listener) {
        getAppointmentsByPatient(dniPaciente, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                if (docs.isEmpty()) {
                    // No hay citas, llamar al listener como éxito
                    if (listener != null) {
                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
                    }
                    return;
                }

                // Eliminar cada cita
                int total = docs.size();
                final int[] completed = {0};
                final boolean[] hasError = {false};
                final Exception[] firstError = {null};

                for (DocumentSnapshot doc : docs) {
                    doc.getReference().delete()
                            .addOnSuccessListener(aVoid -> {
                                completed[0]++;
                                if (completed[0] == total && !hasError[0]) {
                                    if (listener != null) {
                                        listener.onComplete(com.google.android.gms.tasks.Tasks.forResult(null));
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!hasError[0]) {
                                    hasError[0] = true;
                                    firstError[0] = e;
                                    Log.e(TAG, "Error al eliminar cita", e);
                                    if (listener != null) {
                                        listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                                    }
                                }
                            });
                }
            } else {
                if (listener != null) {
                    Exception e = task.getException() != null ? task.getException() : new Exception("Error desconocido");
                    listener.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                }
            }
        });
    }

    /**
     * Convierte un DocumentSnapshot a objeto Cita
     */
    public Cita documentToCita(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }

        Cita cita = new Cita();
        
        // Guardar el ID del documento de Firestore
        cita.setFirestoreId(doc.getId());
        
        // Obtener el ID del documento o del campo
        Object idObj = doc.get("id");
        if (idObj instanceof Long) {
            cita.setId((Long) idObj);
        } else if (idObj instanceof Number) {
            cita.setId(((Number) idObj).longValue());
        } else {
            // Si no hay ID en el documento, usar el ID del documento de Firestore
            cita.setId(System.currentTimeMillis()); // Fallback
        }
        
        cita.setDniPaciente(doc.getString("dniPaciente"));
        cita.setFecha(doc.getString("fecha"));
        cita.setHora(doc.getString("hora"));
        cita.setDoctorNombre(doc.getString("doctorNombre"));
        cita.setMotivo(doc.getString("motivo"));
        
        // Leer estado (por defecto "pendiente" si no existe)
        String estado = doc.getString("estado");
        cita.setEstado(estado != null ? estado : "pendiente");
        
        // Leer información del centro de salud
        String centroNombre = doc.getString("centroNombre");
        if (centroNombre != null && !centroNombre.isEmpty()) {
            cita.setCentroNombre(centroNombre);
            cita.setCentroDireccion(doc.getString("centroDireccion"));
            
            Double centroLat = doc.getDouble("centroLat");
            Double centroLng = doc.getDouble("centroLng");
            if (centroLat != null) cita.setCentroLat(centroLat);
            if (centroLng != null) cita.setCentroLng(centroLng);
        }

        return cita;
    }

    /**
     * Convierte una lista de DocumentSnapshots a lista de Citas
     */
    public List<Cita> documentsToCitas(QuerySnapshot snapshot) {
        List<Cita> citas = new ArrayList<>();
        if (snapshot != null && !snapshot.isEmpty()) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Cita cita = documentToCita(doc);
                if (cita != null) {
                    citas.add(cita);
                }
            }
        }
        return citas;
    }
}

