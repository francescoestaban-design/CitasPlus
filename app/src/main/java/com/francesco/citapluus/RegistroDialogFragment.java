package com.francesco.citapluus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;

public class RegistroDialogFragment extends DialogFragment {

    private EditText editTextDNI, editTextNombre, editTextApellidoPaterno,
            editTextApellidoMaterno, editTextCIPA, editTextCodigoPostal, editTextContrasena;
    private Spinner spinnerTipoSangre;
    private Button buttonRegistrar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_registro, null);

        // Referencias (coinciden con el XML de abajo)
        editTextDNI             = view.findViewById(R.id.editTextDNI);
        editTextNombre          = view.findViewById(R.id.editTextNombre);
        editTextApellidoPaterno = view.findViewById(R.id.editTextApellidoPaterno);
        editTextApellidoMaterno = view.findViewById(R.id.editTextApellidoMaterno);
        spinnerTipoSangre       = view.findViewById(R.id.spinnerTipoSangre);
        editTextCIPA            = view.findViewById(R.id.editTextCIPA);
        editTextCodigoPostal    = view.findViewById(R.id.editTextCodigoPostal);
        editTextContrasena      = view.findViewById(R.id.editTextContrasena);
        buttonRegistrar         = view.findViewById(R.id.buttonRegistrar);

        String[] tiposSangre = {
                "Seleccionar", "A+", "A-", "B+", "B-",
                "AB+", "AB-", "O+", "O-", "No sé"
        };
// Spinner OK
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposSangre
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSangre.setAdapter(adapter);

        spinnerTipoSangre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {}
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Construcción del diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
                .setTitle("Crear Cuenta")
                .setNegativeButton("Cancelar", (DialogInterface dialog, int which) -> {});

        final AlertDialog dialog = builder.create();

        // Acción Registrar
        buttonRegistrar.setOnClickListener(v -> {
            String dni          = txt(editTextDNI);
            String nombre       = txt(editTextNombre);
            String apPaterno    = txt(editTextApellidoPaterno);
            String apMaterno    = txt(editTextApellidoMaterno);
            String tipoSangreSel= spinnerTipoSangre.getSelectedItem() != null
                    ? spinnerTipoSangre.getSelectedItem().toString() : "";
            String cipa         = txt(editTextCIPA);
            String codigoPostal = txt(editTextCodigoPostal);
            String contrasena   = txt(editTextContrasena);

            if (TextUtils.isEmpty(dni) || TextUtils.isEmpty(nombre) ||
                    TextUtils.isEmpty(apPaterno) || TextUtils.isEmpty(apMaterno) ||
                    TextUtils.isEmpty(cipa) || TextUtils.isEmpty(codigoPostal) ||
                    TextUtils.isEmpty(contrasena) || "Seleccionar".equals(tipoSangreSel)) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar en SharedPreferences (tu SessionManager)
            SessionManager sm = new SessionManager(requireContext());
            sm.createSession(dni, nombre, apPaterno, apMaterno, tipoSangreSel, cipa, codigoPostal, contrasena);

            Toast.makeText(requireContext(), "¡Registro exitoso! Ahora inicia sesión.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        return dialog;
    }

    private String txt(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}

