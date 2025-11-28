package com.francesco.citapluus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.widget.EditText;

import com.francesco.citapluus.ui.MainActivity;
import com.francesco.citapluus.ui.SessionManager;
import com.francesco.citapluus.data.UserRepository;
import com.francesco.citapluus.data.Paciente;

public class LoginDialogFragment extends DialogFragment {
    
    // Callback para notificar cuando el login es exitoso
    public interface OnLoginSuccessListener {
        void onLoginSuccess();
    }
    
    private OnLoginSuccessListener loginSuccessListener;
    
    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.loginSuccessListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflar el layout del diálogo
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_login, null);

        // Referencias a los campos
        EditText editTextUsuario = view.findViewById(R.id.editTextUsuario);
        EditText editTextContrasena = view.findViewById(R.id.editTextContrasena);
        Button buttonLogin = view.findViewById(R.id.buttonIniciarSesionDialog);
        Button buttonRegistrarse = view.findViewById(R.id.buttonRegistrarse);

        // Construir el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Solo cerrar
                    }
                });

        final AlertDialog dialog = builder.create();

        // Acción del botón "Registrarse"
        buttonRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                RegistroDialogFragment registroDialog = new RegistroDialogFragment();
                registroDialog.show(getParentFragmentManager(), "registro");
            }
        });

        // Acción del botón "Iniciar Sesión"
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuario = editTextUsuario.getText().toString().trim();
                String contrasena = editTextContrasena.getText().toString().trim();

                // ✅ Validaciones mejoradas
                if (usuario.isEmpty()) {
                    Toast.makeText(getContext(), "Por favor, ingresa tu DNI", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (contrasena.isEmpty()) {
                    Toast.makeText(getContext(), "Por favor, ingresa tu contraseña", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (usuario.length() < 8 && !usuario.equals("admin")) {
                    Toast.makeText(getContext(), "El DNI debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Login válido, proceder
                {
                    SessionManager sessionManager = SessionManager.getInstance(requireContext());
                    UserRepository userRepo = new UserRepository();

                    // ✅ Login de paciente verificando en Firestore
                    {
                        userRepo.verifyLogin(usuario, contrasena, new UserRepository.LoginCallback() {
                            @Override
                            public void onSuccess(com.google.firebase.firestore.DocumentSnapshot userDoc) {
                                // Login exitoso - cargar datos del usuario desde Firestore
                                Paciente paciente = userRepo.documentToPaciente(userDoc);
                                
                                if (paciente != null) {
                                    // Guardar en SessionManager local
                                    sessionManager.savePaciente(paciente);
                                    sessionManager.setLoggedIn(true);
                                    
                                    // ✅ Cargar centro de salud desde Firestore si existe
                                    String centroNombre = userDoc.getString("centroNombre");
                                    String centroDireccion = userDoc.getString("centroDireccion");
                                    Double centroLat = userDoc.getDouble("centroLat");
                                    Double centroLng = userDoc.getDouble("centroLng");
                                    
                                    if (centroNombre != null && !centroNombre.isEmpty() && 
                                        centroLat != null && centroLng != null) {
                                        sessionManager.setCentroSalud(centroNombre, centroDireccion, 
                                                                     centroLat, centroLng);
                                    }

                                    Toast.makeText(getContext(), "¡Bienvenido, " + sessionManager.getNombreCompleto() + "!", Toast.LENGTH_SHORT).show();

                                    // Si ya estamos en MainActivity, solo cerrar el diálogo y notificar
                                    if (getActivity() instanceof MainActivity) {
                                        dialog.dismiss();
                                        // Notificar al listener si existe
                                        if (loginSuccessListener != null) {
                                            loginSuccessListener.onLoginSuccess();
                                        }
                                        // Refrescar la vista de MainActivity
                                        ((MainActivity) getActivity()).refreshView();
                                    } else {
                                        // Si estamos en otra actividad (LoginActivity), navegar a MainActivity
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        intent.putExtra("usuario", sessionManager.getNombreCompleto());
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        dialog.dismiss();
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Error al cargar datos del usuario", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Exception error) {
                                // También verificar login local como fallback
                                if (usuario.equals(sessionManager.getDNI()) && contrasena.equals(sessionManager.getContrasena())) {
                                    // Login local exitoso
                                    sessionManager.setLoggedIn(true);
                                    Toast.makeText(getContext(), "¡Bienvenido, " + sessionManager.getNombreCompleto() + "!", Toast.LENGTH_SHORT).show();

                                    if (getActivity() instanceof MainActivity) {
                                        dialog.dismiss();
                                        if (loginSuccessListener != null) {
                                            loginSuccessListener.onLoginSuccess();
                                        }
                                        ((MainActivity) getActivity()).refreshView();
                                    } else {
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        intent.putExtra("usuario", sessionManager.getNombreCompleto());
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        dialog.dismiss();
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }
            }
        });

        return dialog;
    }
}