package com.francesco.citapluus;

import java.io.Serializable;

public class Cita implements Serializable {
    private long id;
    private String dniPaciente;
    private String fecha;         // "dd/MM/yyyy"
    private String hora;          // "HH:mm"
    private String motivo;
    private String doctorNombre;

    public Cita() { }

    public Cita(long id, String dniPaciente, String fecha, String hora, String motivo, String doctorNombre) {
        this.id = id;
        this.dniPaciente = dniPaciente;
        this.fecha = fecha;
        this.hora = hora;
        this.motivo = motivo;
        this.doctorNombre = doctorNombre;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDniPaciente() { return dniPaciente; }
    public void setDniPaciente(String dniPaciente) { this.dniPaciente = dniPaciente; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDoctorNombre() { return doctorNombre; }
    public void setDoctorNombre(String doctorNombre) { this.doctorNombre = doctorNombre; }
}
