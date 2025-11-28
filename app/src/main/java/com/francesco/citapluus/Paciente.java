package com.francesco.citapluus;

public class Paciente {
    private String nombreCompleto;
    private String dni;
    private String tipoSangre;
    private String cipa;
    private String codigoPostal;

    // ✅ Constructor con 5 parámetros
    public Paciente(String nombreCompleto, String dni, String tipoSangre, String cipa, String codigoPostal) {
        this.nombreCompleto = nombreCompleto;
        this.dni = dni;
        this.tipoSangre = tipoSangre;
        this.cipa = cipa;
        this.codigoPostal = codigoPostal;
    }

    // Getters
    public String getNombreCompleto() { return nombreCompleto; }
    public String getDni() { return dni; }
    public String getTipoSangre() { return tipoSangre; }
    public String getCipa() { return cipa; }
    public String getCodigoPostal() { return codigoPostal; }
}