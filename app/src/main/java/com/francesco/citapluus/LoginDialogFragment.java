package com.francesco.citapluus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;
import com.google.android.material.textfield.TextInputEditText;
import com.francesco.citapluus.SessionManager; // ✅ ¡ESTA LÍNEA ES CLAVE!

public class LoginDialogFragment extends DialogFragment {
    // ... resto del código

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflar el layout del diálogo
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_login, null);

        // Referencias a los campos
        TextInputEditText editTextUsuario = view.findViewById(R.id.editTextUsuario);
        TextInputEditText editTextContrasena = view.findViewById(R.id.editTextContrasena);
        Button buttonLogin = view.findViewById(R.id.buttonIniciarSesionDialog);

        // Construir el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setTitle("Acceso al Sistema")
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Solo cerrar
                    }
                });

        final AlertDialog dialog = builder.create();

        // Acción del botón "Iniciar Sesión"
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuario = editTextUsuario.getText().toString().trim();
                String contrasena = editTextContrasena.getText().toString().trim();

                if (usuario.isEmpty() || contrasena.isEmpty()) {
                    Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    // ✅ DECLARAR E INICIALIZAR sessionManager
                    SessionManager sessionManager = new SessionManager(getContext());

                    // ✅ Simular login de trabajador (DNI: "admin", Contraseña: "1234")
                    if (usuario.equals("admin") && contrasena.equals("1234")) {
                        Toast.makeText(getContext(), "¡Bienvenido, Administrador!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getActivity(), WorkerMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                        getActivity().finish();
                    }
// ✅ Login de paciente (usuario registrado)
                    else if (usuario.equals(sessionManager.getDNI()) && contrasena.equals(sessionManager.getContrasena())) {
                        Toast.makeText(getContext(), "¡Bienvenido, " + sessionManager.getNombreCompleto() + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.putExtra("usuario", sessionManager.getNombreCompleto());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                        getActivity().finish();
                    } else {
                        Toast.makeText(getContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        return dialog;
    }
}