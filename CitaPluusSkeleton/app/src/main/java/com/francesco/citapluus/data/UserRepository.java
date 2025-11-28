package com.francesco.citapluus.data;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USUARIOS = "usuarios";
    
    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseService.getInstance().getDb();
    }

    /**
     * Guarda un usuario en Firestore
     */
    public void saveUser(Paciente paciente, String contrasena, OnCompleteListener<Void> listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("dni", paciente.getDni());
        userData.put("nombre", paciente.getNombre());
        userData.put("apellidoPaterno", paciente.getApellidoPaterno());
        userData.put("apellidoMaterno", paciente.getApellidoMaterno());
        userData.put("tipoSangre", paciente.getTipoSangre());
        userData.put("email", paciente.getEmail() != null ? paciente.getEmail() : "");  // AÑADIDO: Guardar email
        userData.put("cipa", paciente.getCipa());
        userData.put("codigoPostal", paciente.getCodigoPostal());
        userData.put("alergias", paciente.getAlergias() != null ? paciente.getAlergias() : "");
        userData.put("contrasena", hashPassword(contrasena)); // Guardar hash de la contraseña
        userData.put("fechaRegistro", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_USUARIOS)
                .document(paciente.getDni())
                .set(userData)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar usuario", e);
                });
    }

    /**
     * Obtiene un usuario por DNI
     */
    public void getUserByDni(String dni, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection(COLLECTION_USUARIOS)
                .document(dni)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener usuario", e);
                });
    }

    /**
     * Verifica las credenciales de login
     * @param dni DNI del usuario
     * @param contrasena Contraseña sin hashear
     * @param callback Callback con el resultado (true si login exitoso, false si falla)
     */
    public void verifyLogin(String dni, String contrasena, LoginCallback callback) {
        getUserByDni(dni, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    String storedPassword = doc.getString("contrasena");
                    String hashedInput = hashPassword(contrasena);
                    
                    if (hashedInput.equals(storedPassword)) {
                        // Contraseña correcta
                        callback.onSuccess(doc);
                    } else {
                        // Contraseña incorrecta
                        callback.onFailure(new Exception("Contraseña incorrecta"));
                    }
                } else {
                    // Usuario no existe
                    callback.onFailure(new Exception("Usuario no encontrado"));
                }
            } else {
                // Error al obtener usuario
                callback.onFailure(task.getException() != null ? task.getException() : new Exception("Error desconocido"));
            }
        });
    }

    /**
     * Callback para el resultado del login
     */
    public interface LoginCallback {
        void onSuccess(DocumentSnapshot userDoc);
        void onFailure(Exception error);
    }

    /**
     * Actualiza los datos de un usuario
     */
    public void updateUser(String dni, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_USUARIOS)
                .document(dni)
                .update(updates)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar usuario", e);
                });
    }

    /**
     * Elimina un usuario
     */
    public void deleteUser(String dni, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_USUARIOS)
                .document(dni)
                .delete()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar usuario", e);
                });
    }

    /**
     * Convierte un DocumentSnapshot a objeto Paciente
     */
    public Paciente documentToPaciente(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }

        Paciente paciente = new Paciente();
        paciente.setDni(doc.getString("dni"));
        paciente.setNombre(doc.getString("nombre"));
        paciente.setApellidoPaterno(doc.getString("apellidoPaterno"));
        paciente.setApellidoMaterno(doc.getString("apellidoMaterno"));
        paciente.setTipoSangre(doc.getString("tipoSangre"));
        paciente.setEmail(doc.getString("email"));  // AÑADIDO: Recuperar email
        paciente.setCipa(doc.getString("cipa"));
        paciente.setCodigoPostal(doc.getString("codigoPostal"));
        paciente.setAlergias(doc.getString("alergias"));

        return paciente;
    }

    /**
     * Hash simple de contraseña (SHA-256)
     * En producción, usar bcrypt o similar
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error al hashear contraseña", e);
            return password; // Fallback (no recomendado en producción)
        }
    }
}

