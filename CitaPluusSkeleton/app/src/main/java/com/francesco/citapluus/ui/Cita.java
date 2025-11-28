package com.francesco.citapluus.ui;

import java.io.Serializable;

public class Cita implements Serializable {
    private long id;
    private String firestoreId;   // ID del documento en Firestore
    private String dniPaciente;
    private String fecha;         // "dd/MM/yyyy"
    private String hora;          // "HH:mm"
    private String motivo;
    private String doctorNombre;
    private String estado;        // "pendiente", "completada", "cancelada"
    
    // Centro de salud donde se realizará la cita
    private String centroNombre;
    private String centroDireccion;
    private double centroLat;
    private double centroLng;

    public Cita() { 
        this.estado = "pendiente"; // Por defecto
    }

    public Cita(long id, String dniPaciente, String fecha, String hora, String motivo, String doctorNombre) {
        this.id = id;
        this.dniPaciente = dniPaciente;
        this.fecha = fecha;
        this.hora = hora;
        this.motivo = motivo;
        this.doctorNombre = doctorNombre;
        this.estado = "pendiente";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCentroNombre() { return centroNombre; }
    public void setCentroNombre(String centroNombre) { this.centroNombre = centroNombre; }

    public String getCentroDireccion() { return centroDireccion; }
    public void setCentroDireccion(String centroDireccion) { this.centroDireccion = centroDireccion; }

    public double getCentroLat() { return centroLat; }
    public void setCentroLat(double centroLat) { this.centroLat = centroLat; }

    public double getCentroLng() { return centroLng; }
    public void setCentroLng(double centroLng) { this.centroLng = centroLng; }
}
