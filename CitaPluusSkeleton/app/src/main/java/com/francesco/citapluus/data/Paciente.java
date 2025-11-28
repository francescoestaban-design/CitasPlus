package com.francesco.citapluus.data;

public class Paciente {

    private String dni;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String tipoSangre;
    private String email;  // AÑADIDO: Campo de email
    private String cipa;
    private String codigoPostal;
    private String alergias;

    // Constructor vacío obligatorio para Firestore
    public Paciente() {}

    public Paciente(String dni, String nombre, String apellidoPaterno, String apellidoMaterno,
                    String tipoSangre, String email, String cipa, String codigoPostal, String alergias) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.tipoSangre = tipoSangre;
        this.email = email;  // AÑADIDO
        this.cipa = cipa;
        this.codigoPostal = codigoPostal;
        this.alergias = alergias;
    }

    // GETTERS
    public String getDni() { return dni; }
    public String getNombre() { return nombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getTipoSangre() { return tipoSangre; }
    public String getEmail() { return email; }  // AÑADIDO
    public String getCipa() { return cipa; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getAlergias() { return alergias; }

    // SETTERS
    public void setDni(String dni) { this.dni = dni; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }
    public void setEmail(String email) { this.email = email; }  // AÑADIDO
    public void setCipa(String cipa) { this.cipa = cipa; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
}
