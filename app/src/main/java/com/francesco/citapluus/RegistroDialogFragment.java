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
import com.francesco.citapluus.ui.SessionManager;
import com.francesco.citapluus.R;

public class RegistroDialogFragment extends DialogFragment {

    private EditText editTextDNI, editTextNombre, editTextApellidoPaterno,
            editTextApellidoMaterno, editTextCIPA, editTextCodigoPostal,
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
        editTextCIPA             = view.findViewById(R.id.editTextCIPA);
        editTextCodigoPostal     = view.findViewById(R.id.editTextCodigoPostal);
        editTextAlergias         = view.findViewById(R.id.editTextAlergias);
        editTextContrasena       = view.findViewById(R.id.editTextContrasena);
        buttonRegistrar          = view.findViewById(R.id.buttonRegistrar);

        // Spinner
        String[] tipos = {"Seleccionar","A+","A-","B+","B-","AB+","AB-","O+","O-","No sé"};
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
        String cipa      = getTxt(editTextCIPA);
        String codPostal = getTxt(editTextCodigoPostal);
        String alergias  = getTxt(editTextAlergias);
        String pass      = getTxt(editTextContrasena);

        if (dni.isEmpty() || nombre.isEmpty() || apPat.isEmpty() || apMat.isEmpty()
                || cipa.isEmpty() || codPostal.isEmpty() || pass.isEmpty()
                || tipoSangre.equals("Seleccionar")) {

            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto Paciente
        Paciente p = new Paciente(
                dni, nombre, apPat, apMat, tipoSangre,
                cipa, codPostal, alergias
        );

        // Guardar localmente
        SessionManager.getInstance(getContext()).savePaciente(p);

        // Guardar en Firestore
        FirebaseService.getInstance().getDb()
                .collection("pacientes")
                .document(dni)
                .set(p)
                .addOnSuccessListener(a ->
                        Toast.makeText(getContext(), "Paciente registrado correctamente", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String getTxt(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
