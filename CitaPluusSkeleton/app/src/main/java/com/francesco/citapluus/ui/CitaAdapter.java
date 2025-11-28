package com.francesco.citapluus.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.francesco.citapluus.CitaManager;
import com.francesco.citapluus.ui.Cita;

import com.francesco.citapluus.R;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {

    private Context context;
    private List<Cita> listaCitas;
    private OnItemClickListener onItemClickListener;

    // Interface para click en item
    public interface OnItemClickListener {
        void onItemClick(Cita cita);
    }

    public CitaAdapter(List<Cita> listaCitas) {
        this.listaCitas = listaCitas;
    }

    public CitaAdapter(Context context, List<Cita> listaCitas) {
        this.context = context;
        this.listaCitas = listaCitas;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_cita, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        Cita cita = listaCitas.get(position);
        holder.textViewFecha.setText(cita.getFecha());
        holder.textViewHora.setText(cita.getHora());
        holder.textViewDoctor.setText(cita.getDoctorNombre());
        holder.textViewMotivo.setText(cita.getMotivo());

        // Mostrar estado si existe
        String estado = cita.getEstado();
        if (estado != null && !estado.isEmpty()) {
            holder.textViewEstado.setVisibility(View.VISIBLE);
            holder.textViewEstado.setText(capitalize(estado));
            
            // Cambiar color de fondo y texto según estado
            switch (estado.toLowerCase()) {
                case "pendiente":
                    holder.textViewEstado.setTextColor(Color.parseColor("#F57C00"));
                    holder.textViewEstado.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    break;
                case "completada":
                    holder.textViewEstado.setTextColor(Color.parseColor("#388E3C"));
                    holder.textViewEstado.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    break;
                case "cancelada":
                    holder.textViewEstado.setTextColor(Color.parseColor("#D32F2F"));
                    holder.textViewEstado.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    break;
                default:
                    holder.textViewEstado.setTextColor(Color.parseColor("#757575"));
                    holder.textViewEstado.setBackgroundColor(Color.parseColor("#F5F5F5"));
            }
        } else {
            holder.textViewEstado.setVisibility(View.GONE);
        }

        // Mostrar centro de salud si existe
        if (cita.getCentroNombre() != null && !cita.getCentroNombre().isEmpty()) {
            holder.textViewCentro.setVisibility(View.VISIBLE);
            holder.textViewCentro.setText(cita.getCentroNombre());
        } else {
            holder.textViewCentro.setVisibility(View.GONE);
        }

        // Click para ver detalles
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(cita);
            }
        });

        // Botón cancelar solo visible si está pendiente
        if ("pendiente".equals(estado)) {
            holder.buttonCancelar.setVisibility(View.VISIBLE);
            holder.buttonCancelar.setOnClickListener(v -> {
                // ✅ Mostrar diálogo de confirmación antes de cancelar
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Cancelar Cita")
                        .setMessage("¿Estás seguro de que deseas cancelar esta cita con " + cita.getDoctorNombre() + "?")
                        .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                            // Cambiar estado a cancelada en vez de eliminar
                            cita.setEstado("cancelada");
                            CitaManager.getInstance().actualizarCita(cita);
                            
                            // Cancelar notificación
                            NotificationManagerCompat.from(context).cancel((int) cita.getId());
                            
                            // Actualizar UI
                            notifyItemChanged(position);
                            
                            android.widget.Toast.makeText(context, "Cita cancelada", android.widget.Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        } else {
            holder.buttonCancelar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return listaCitas.size();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView textViewFecha, textViewHora, textViewDoctor, textViewMotivo, textViewEstado, textViewCentro;
        Button buttonCancelar;

        public CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFecha = itemView.findViewById(R.id.textViewFecha);
            textViewHora = itemView.findViewById(R.id.textViewHora);
            textViewDoctor = itemView.findViewById(R.id.textViewDoctor);
            textViewMotivo = itemView.findViewById(R.id.textViewMotivo);
            textViewEstado = itemView.findViewById(R.id.textViewEstado);
            textViewCentro = itemView.findViewById(R.id.textViewCentro);
            buttonCancelar = itemView.findViewById(R.id.buttonCancelar);
        }
    }
}
