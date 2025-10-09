package com.francesco.citapluus;

import java.util.ArrayList;
import java.util.List;

public class CitaManager {
    private static CitaManager instance;
    private final List<Cita> citas = new ArrayList<>();

    private CitaManager() {}

    public static synchronized CitaManager getInstance() {
        if (instance == null) instance = new CitaManager();
        return instance;
    }

    /** Añade una cita; si no trae id, se genera uno simple. */
    public void agendarCita(Cita cita) {
        if (cita == null) return;
        if (cita.getId() == 0L) {
            cita.setId(System.currentTimeMillis());
        }
        citas.add(cita);
    }

    /** Actualiza la cita que tenga el mismo id. */
    public void actualizarCita(Cita cita) {
        if (cita == null) return;
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getId() == cita.getId()) {
                citas.set(i, cita);
                return;
            }
        }
    }

    /** Devuelve todas las citas del paciente por DNI. */
    public List<Cita> obtenerCitasPorPaciente(String dniPaciente) {
        List<Cita> out = new ArrayList<>();
        if (dniPaciente == null) return out;
        for (Cita c : citas) {
            if (dniPaciente.equals(c.getDniPaciente())) {
                out.add(c);
            }
        }
        return out;
    }

    /** Elimina por id (⚠️ aquí estaba el fallo: getIdUnico() no existe). */
    public void cancelarCita(long idCita) {
        citas.removeIf(c -> c.getId() == idCita);
    }

    /** (Opcional) Buscar por id. */
    public Cita obtenerCitaPorId(long idCita) {
        for (Cita c : citas) if (c.getId() == idCita) return c;
        return null;
    }
}
