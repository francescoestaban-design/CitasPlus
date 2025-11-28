package com.francesco.citapluus;

import java.util.ArrayList;
import java.util.List;

import com.francesco.citapluus.ui.Cita;

public class CitaManager {

    private static CitaManager instance = null;
    private final List<Cita> citas = new ArrayList<>();

    public static synchronized CitaManager getInstance() {
        if (instance == null) instance = new CitaManager();
        return instance;
    }

    // AGREGAR CITA
    public void agregarCita(Cita cita) {
        citas.add(cita);
    }

    // REVISAR SI EXISTE CITA EN FECHA+HORA
    public boolean existeCita(String fecha, String hora) {
        for (Cita c : citas) {
            if (c.getFecha().equals(fecha) && c.getHora().equals(hora)) {
                return true;
            }
        }
        return false;
    }

    // CANCELAR CITA
    public void cancelarCita(long id) {
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getId() == id) {
                citas.remove(i);
                return;
            }
        }
    }

    // ACTUALIZAR CITA (NECESARIO PARA EDITAR)
    public void actualizarCita(Cita citaActualizada) {
        for (int i = 0; i < citas.size(); i++) {
            if (citas.get(i).getId() == citaActualizada.getId()) {
                citas.set(i, citaActualizada);
                return;
            }
        }
    }

    // LISTA DE CITAS
    public List<Cita> getCitas() {
        return citas;
    }
}
