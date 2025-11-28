package com.francesco.citapluus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.francesco.citapluus.data.FirebaseService;
import com.francesco.citapluus.data.Paciente;
import com.francesco.citapluus.data.UserRepository;
import com.francesco.citapluus.ui.SessionManager;
import com.francesco.citapluus.R;

public class RegistroDialogFragment extends DialogFragment {

    private EditText editTextDNI, editTextNombre, editTextApellidoPaterno,
            editTextApellidoMaterno, editTextEmail, editTextCodigoPostal,
            editTextAlergias, editTextContrasena;

    private Spinner spinnerTipoSangre;
    private Button buttonRegistrar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_registro, null);

        // Referencias UI
        editTextDNI              = view.findViewById(R.id.editTextDNI);
        editTextNombre           = view.findViewById(R.id.editTextNombre);
        editTextApellidoPaterno  = view.findViewById(R.id.editTextApellidoPaterno);
        editTextApellidoMaterno  = view.findViewById(R.id.editTextApellidoMaterno);
        spinnerTipoSangre        = view.findViewById(R.id.spinnerTipoSangre);
        editTextEmail            = view.findViewById(R.id.editTextEmail);
        editTextCodigoPostal     = view.findViewById(R.id.editTextCodigoPostal);
        editTextAlergias         = view.findViewById(R.id.editTextAlergias);
        editTextContrasena       = view.findViewById(R.id.editTextContrasena);
        buttonRegistrar          = view.findViewById(R.id.buttonRegistrar);

        // Spinner
        String[] tipos = {"Tipo de sangre","A+","A-","B+","B-","AB+","AB-","O+","O-","No lo sé"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, tipos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSangre.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
                .setTitle("Crear Cuenta")
                .setNegativeButton("Cancelar", (dialog, which) -> {});

        AlertDialog dialog = builder.create();

        buttonRegistrar.setOnClickListener(v -> registrarPaciente());

        return dialog;
    }

    private void registrarPaciente() {

        String dni       = getTxt(editTextDNI);
        String nombre    = getTxt(editTextNombre);
        String apPat     = getTxt(editTextApellidoPaterno);
        String apMat     = getTxt(editTextApellidoMaterno);
        String tipoSangre= spinnerTipoSangre.getSelectedItem().toString();
        String email     = getTxt(editTextEmail);
        String codPostal = getTxt(editTextCodigoPostal);
        String alergias  = getTxt(editTextAlergias);
        String pass      = getTxt(editTextContrasena);

        // ✅ Validaciones mejoradas con mensajes específicos
        
        // DNI: Solo letras y números, sin caracteres especiales
        if (dni.isEmpty()) {
            Toast.makeText(getContext(), "El DNI es obligatorio", Toast.LENGTH_SHORT).show();
            editTextDNI.requestFocus();
            return;
        }
        
        if (dni.length() < 8) {
            Toast.makeText(getContext(), "El DNI debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
            editTextDNI.requestFocus();
            return;
        }
        
        if (!dni.matches("[a-zA-Z0-9]+")) {
            Toast.makeText(getContext(), "El DNI solo puede contener letras y números", Toast.LENGTH_SHORT).show();
            editTextDNI.requestFocus();
            return;
        }
        
        // NOMBRE: Solo letras y ñ
        if (nombre.isEmpty()) {
            Toast.makeText(getContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            editTextNombre.requestFocus();
            return;
        }
        
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
            Toast.makeText(getContext(), "El nombre solo puede contener letras (sin números ni símbolos)", Toast.LENGTH_SHORT).show();
            editTextNombre.requestFocus();
            return;
        }
        
        // APELLIDO PATERNO: Solo letras y ñ
        if (apPat.isEmpty()) {
            Toast.makeText(getContext(), "El apellido paterno es obligatorio", Toast.LENGTH_SHORT).show();
            editTextApellidoPaterno.requestFocus();
            return;
        }
        
        if (!apPat.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
            Toast.makeText(getContext(), "El apellido paterno solo puede contener letras (sin números ni símbolos)", Toast.LENGTH_SHORT).show();
            editTextApellidoPaterno.requestFocus();
            return;
        }
        
        // APELLIDO MATERNO: Solo letras y ñ
        if (apMat.isEmpty()) {
            Toast.makeText(getContext(), "El apellido materno es obligatorio", Toast.LENGTH_SHORT).show();
            editTextApellidoMaterno.requestFocus();
            return;
        }
        
        if (!apMat.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+")) {
            Toast.makeText(getContext(), "El apellido materno solo puede contener letras (sin números ni símbolos)", Toast.LENGTH_SHORT).show();
            editTextApellidoMaterno.requestFocus();
            return;
        }
        
        // TIPO DE SANGRE
        if (tipoSangre.equals("Tipo de sangre")) {
            Toast.makeText(getContext(), "Selecciona tu tipo de sangre", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // EMAIL
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "El email es obligatorio", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            Toast.makeText(getContext(), "Ingresa un email válido", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }
        
        // CÓDIGO POSTAL: Exactamente 5 dígitos
        if (codPostal.isEmpty()) {
            Toast.makeText(getContext(), "El código postal es obligatorio", Toast.LENGTH_SHORT).show();
            editTextCodigoPostal.requestFocus();
            return;
        }
        
        if (codPostal.length() != 5) {
            Toast.makeText(getContext(), "El código postal debe tener exactamente 5 dígitos", Toast.LENGTH_SHORT).show();
            editTextCodigoPostal.requestFocus();
            return;
        }
        
        if (!codPostal.matches("[0-9]{5}")) {
            Toast.makeText(getContext(), "El código postal solo puede contener números", Toast.LENGTH_SHORT).show();
            editTextCodigoPostal.requestFocus();
            return;
        }
        
        // ALERGIAS: Solo letras, números, comas y espacios
        if (!alergias.isEmpty() && !alergias.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9, ]+")) {
            Toast.makeText(getContext(), "Las alergias solo pueden contener letras, números y comas", Toast.LENGTH_SHORT).show();
            editTextAlergias.requestFocus();
            return;
        }
        
        // CONTRASEÑA
        if (pass.isEmpty()) {
            Toast.makeText(getContext(), "La contraseña es obligatoria", Toast.LENGTH_SHORT).show();
            editTextContrasena.requestFocus();
            return;
        }
        
        if (pass.length() < 4) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show();
            editTextContrasena.requestFocus();
            return;
        }

        // Crear objeto Paciente (con email incluido)
        Paciente p = new Paciente(
                dni, nombre, apPat, apMat, tipoSangre,
                email,  // AÑADIDO: Pasar el email al constructor
                dni, codPostal, alergias  // Usar DNI como CIPA temporalmente para compatibilidad
        );

        // Guardar en Firestore usando UserRepository
        UserRepository userRepo = new UserRepository();
        userRepo.saveUser(p, pass, task -> {
            if (task.isSuccessful()) {
                // Guardar también localmente
                SessionManager sm = SessionManager.getInstance(getContext());
                sm.savePaciente(p);
                sm.saveContrasena(pass);
                sm.setLoggedIn(true);

                Toast.makeText(getContext(), "Registro exitoso. Bienvenido " + nombre + "!", Toast.LENGTH_SHORT).show();
                
                dismiss();
                
                // Refrescar la vista de MainActivity si ya estamos ahí
                if (getActivity() instanceof com.francesco.citapluus.ui.MainActivity) {
                    ((com.francesco.citapluus.ui.MainActivity) getActivity()).refreshView();
                } else {
                    // Si estamos en LoginActivity, navegar a MainActivity
                    android.content.Intent intent = new android.content.Intent(getActivity(), com.francesco.citapluus.ui.MainActivity.class);
                    intent.putExtra("usuario", nombre);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            } else {
                // Error al guardar en Firestore
                Toast.makeText(getContext(), "Error al registrar: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Error desconocido"), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getTxt(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
