package com.francesco.citapluus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.francesco.citapluus.Cita;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {

    private List<Cita> listaCitas;

    public CitaAdapter(List<Cita> listaCitas) {
        this.listaCitas = listaCitas;
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cita, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        Cita cita = listaCitas.get(position);
        holder.textViewFecha.setText("Fecha: " + cita.getFecha());
        holder.textViewHora.setText("Hora: " + cita.getHora());
        holder.textViewDoctor.setText("Doctor: " + cita.getDoctorNombre());
        holder.textViewMotivo.setText("Motivo: " + cita.getMotivo());

        holder.buttonCancelar.setOnClickListener(v -> {
            // Cancelar cita
            CitaManager.getInstance().cancelarCita(cita.getId());
            // Cancelar notificación
            NotificationManagerCompat.from(holder.itemView.getContext()).cancel((int) cita.getId());
            // Actualizar lista
            listaCitas.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return listaCitas.size();
    }

    public static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFecha, textViewHora, textViewDoctor, textViewMotivo;
        Button buttonCancelar;

        public CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFecha = itemView.findViewById(R.id.textViewFecha);
            textViewHora = itemView.findViewById(R.id.textViewHora);
            textViewDoctor = itemView.findViewById(R.id.textViewDoctor);
            textViewMotivo = itemView.findViewById(R.id.textViewMotivo);
            buttonCancelar = itemView.findViewById(R.id.buttonCancelar);
        }
    }
}