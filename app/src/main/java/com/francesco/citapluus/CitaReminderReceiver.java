package com.francesco.citapluus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class CitaReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "CITA_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        String mensaje = intent.getStringExtra("mensaje");
        String titulo = intent.getStringExtra("titulo");

        // Crear canal de notificación (si no existe)
        crearCanalNotificacion(context);

        // Crear Intent para abrir la app al hacer clic en la notificación
        Intent intentApp = new Intent(context, MainActivity.class);
        intentApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intentApp,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Construir notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Obtener NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // ✅ Verificar permisos de notificación (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            } else {
                android.util.Log.w("CitaReminder", "Notificaciones deshabilitadas por el usuario");
            }
        } else {
            // ✅ En versiones anteriores a Android 13, no se requiere verificación explícita
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void crearCanalNotificacion(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorio de Cita";
            String description = "Canal para recordatorios de citas médicas";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}