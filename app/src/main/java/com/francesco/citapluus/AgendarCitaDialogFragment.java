package com.francesco.citapluus;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.Calendar;

public class AgendarCitaDialogFragment extends DialogFragment {

    private static final String ARG_CITA  = "arg_cita";
    private static final String ARG_FECHA = "arg_fecha"; // "dd/MM/yyyy"

    private TextView textFecha;
    private Spinner spinnerHora, spinnerMotivo, spinnerDoctor;
    private Button buttonConfirmar;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_agendar_cita, null, false);

        textFecha       = view.findViewById(R.id.textViewFechaSeleccionada);
        spinnerHora     = view.findViewById(R.id.spinnerHora);
        spinnerMotivo   = view.findViewById(R.id.spinnerMotivo);
        spinnerDoctor   = view.findViewById(R.id.spinnerDoctor);
        buttonConfirmar = view.findViewById(R.id.buttonConfirmarCita);

        // --- Adapters (usa android.R) ---
        String[] horas = {"09:00","10:00","11:00","12:00","15:00","16:00","17:00"};
        ArrayAdapter<String> adHoras = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, horas);
        adHoras.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHora.setAdapter(adHoras);

        String[] motivos = {"Consulta general","Chequeo anual","Dolor de cabeza","Fiebre","Vacunación"};
        ArrayAdapter<String> adMotivos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, motivos);
        adMotivos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMotivo.setAdapter(adMotivos);

        String[] doctores = {"Dr. Martínez","Dra. López","Dr. García","Dra. Fernández"};
        ArrayAdapter<String> adDoctores = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, doctores);
        adDoctores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctor.setAdapter(adDoctores);

        // --- Modo edición vs nueva ---
        Cita citaEdit = null;
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_CITA)) {
            try { citaEdit = (Cita) args.getSerializable(ARG_CITA); } catch (Exception ignored) {}
        }

        if (citaEdit != null) {
            textFecha.setText(citaEdit.getFecha());
            preseleccionar(spinnerHora,   citaEdit.getHora());
            preseleccionar(spinnerMotivo, citaEdit.getMotivo());
            preseleccionar(spinnerDoctor, citaEdit.getDoctorNombre());
        } else {
            String fecha = (args != null) ? args.getString(ARG_FECHA, "") : "";
            textFecha.setText(fecha);
        }

        // --- DatePicker al tocar la fecha ---
        textFecha.setOnClickListener(v -> abrirDatePicker());

        // --- Confirmar ---
        Cita finalCitaEdit = citaEdit;
        buttonConfirmar.setOnClickListener(v -> {
            String fecha  = textFecha.getText().toString().trim();
            String hora   = (String) spinnerHora.getSelectedItem();
            String motivo = (String) spinnerMotivo.getSelectedItem();
            String doctor = (String) spinnerDoctor.getSelectedItem();

            if (fecha.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una fecha", Toast.LENGTH_SHORT).show();
                return;
            }

            if (esFechaPasada(fecha)) {
                Toast.makeText(requireContext(), "La fecha no puede ser pasada", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionManager sm = new SessionManager(requireContext());
            String dni = sm.getDNI();

            if (finalCitaEdit == null) {
                // Crear nueva
                Cita nueva = new Cita();
                nueva.setDniPaciente(dni);
                nueva.setFecha(fecha);
                nueva.setHora(hora);
                nueva.setMotivo(motivo);
                nueva.setDoctorNombre(doctor);

                CitaManager.getInstance().agendarCita(nueva);
                programarRecordatorio(nueva);
                Toast.makeText(requireContext(), "Cita creada", Toast.LENGTH_SHORT).show();
            } else {
                // Cancelar alarma antigua si cambió fecha/hora
                cancelarRecordatorio(finalCitaEdit);

                // Actualizar existente
                finalCitaEdit.setHora(hora);
                finalCitaEdit.setMotivo(motivo);
                finalCitaEdit.setDoctorNombre(doctor);
                finalCitaEdit.setFecha(fecha);

                CitaManager.getInstance().actualizarCita(finalCitaEdit);
                programarRecordatorio(finalCitaEdit);
                Toast.makeText(requireContext(), "Cita actualizada", Toast.LENGTH_SHORT).show();
            }

            dismissAllowingStateLoss();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Agendar cita")
                .setView(view)
                .setCancelable(true)
                .create();
    }

    // --- Helpers ---

    private void abrirDatePicker() {
        // Inicial: fecha mostrada o hoy
        int d, m, y;
        try {
            String[] f = textFecha.getText().toString().split("/");
            d = Integer.parseInt(f[0]);
            m = Integer.parseInt(f[1]) - 1;
            y = Integer.parseInt(f[2]);
        } catch (Exception e) {
            Calendar c = Calendar.getInstance();
            d = c.get(Calendar.DAY_OF_MONTH);
            m = c.get(Calendar.MONTH);
            y = c.get(Calendar.YEAR);
        }

        DatePickerDialog dp = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String seleccion = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    textFecha.setText(seleccion);
                },
                y, m, d
        );

        // Bloquear fechas pasadas
        Calendar min = Calendar.getInstance();
        min.set(Calendar.HOUR_OF_DAY, 0);
        min.set(Calendar.MINUTE, 0);
        min.set(Calendar.SECOND, 0);
        min.set(Calendar.MILLISECOND, 0);
        dp.getDatePicker().setMinDate(min.getTimeInMillis());

        dp.show();
    }

    private boolean esFechaPasada(String ddMMyyyy) {
        try {
            String[] f = ddMMyyyy.split("/");
            int d = Integer.parseInt(f[0]);
            int m = Integer.parseInt(f[1]) - 1;
            int y = Integer.parseInt(f[2]);

            Calendar hoy = Calendar.getInstance();
            hoy.set(Calendar.HOUR_OF_DAY, 0);
            hoy.set(Calendar.MINUTE, 0);
            hoy.set(Calendar.SECOND, 0);
            hoy.set(Calendar.MILLISECOND, 0);

            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d, 0, 0, 0);
            sel.set(Calendar.MILLISECOND, 0);

            return sel.before(hoy);
        } catch (Exception e) {
            return false;
        }
    }

    private void preseleccionar(Spinner sp, String valor) {
        if (valor == null) return;
        ArrayAdapter<?> ad = (ArrayAdapter<?>) sp.getAdapter();
        for (int i = 0; i < ad.getCount(); i++) {
            if (valor.equals(ad.getItem(i))) {
                sp.setSelection(i);
                break;
            }
        }
    }

    private int buildRequestCode(Cita c) {
        return (c.getFecha() + "_" + c.getHora() + "_" + c.getDniPaciente()).hashCode();
    }

    private void cancelarRecordatorio(Cita cita) {
        try {
            Context ctx = requireContext().getApplicationContext();
            Intent intent = new Intent(ctx, CitaReminderReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, buildRequestCode(cita), intent, flags);

            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null && pi != null) {
                am.cancel(pi);
            }
        } catch (Exception ignored) { }
    }

    private void programarRecordatorio(Cita cita) {
        try {
            String[] f = cita.getFecha().split("/");
            String[] h = cita.getHora().split(":");
            int day    = Integer.parseInt(f[0]);
            int month0 = Integer.parseInt(f[1]) - 1;
            int year   = Integer.parseInt(f[2]);
            int hour   = Integer.parseInt(h[0]);
            int min    = Integer.parseInt(h[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month0);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long triggerAt = cal.getTimeInMillis();
            Context ctx = requireContext().getApplicationContext();

            Intent intent = new Intent(ctx, CitaReminderReceiver.class);
            intent.putExtra("mensaje", "Tienes una cita: " + cita.getMotivo() + " con " + cita.getDoctorNombre());
            intent.putExtra("dni", cita.getDniPaciente());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

            PendingIntent pi = PendingIntent.getBroadcast(ctx, buildRequestCode(cita), intent, flags);

            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    } else {
                        Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:" + ctx.getPackageName()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(settingsIntent);
                        am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi); // fallback NO exacto
                    }
                } catch (SecurityException ignored) {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Factory methods ---
    public static AgendarCitaDialogFragment newInstance(String fecha_ddMMyyyy) {
        AgendarCitaDialogFragment f = new AgendarCitaDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_FECHA, fecha_ddMMyyyy);
        f.setArguments(b);
        return f;
    }

    public static AgendarCitaDialogFragment newEditInstance(Cita cita) {
        AgendarCitaDialogFragment f = new AgendarCitaDialogFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_CITA, (Serializable) cita);
        f.setArguments(b);
        return f;
    }
}
