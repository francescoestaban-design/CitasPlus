package com.francesco.citapluus;

import java.util.ArrayList;
import java.util.List;

public class DrugInfo {
    private String nombre;
    private String paraQueSirve;
    private String comoTomarlo;
    private String advertencias;
    private String efectosSecundarios;
    private List<String> alergenos = new ArrayList<>();

    // Enlaces opcionales a CIMA
    private String urlProspecto;     // null si no hay
    private String urlFichaTecnica;  // null si no hay

    // Constructor base
    public DrugInfo(String nombre, String paraQueSirve, String comoTomarlo,
                    String advertencias, String efectosSecundarios, List<String> alergenos) {
        this.nombre = safe(nombre);
        this.paraQueSirve = safe(paraQueSirve);
        this.comoTomarlo = safe(comoTomarlo);
        this.advertencias = safe(advertencias);
        this.efectosSecundarios = safe(efectosSecundarios);
        if (alergenos != null) this.alergenos = alergenos;
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getParaQueSirve() { return paraQueSirve; }
    public String getComoTomarlo() { return comoTomarlo; }
    public String getAdvertencias() { return advertencias; }
    public String getEfectosSecundarios() { return efectosSecundarios; }
    public List<String> getAlergenos() { return alergenos; }

    // Setters puntuales
    public void setAlergenos(List<String> alergenos) {
        this.alergenos = (alergenos != null) ? alergenos : new ArrayList<>();
    }

    // URLs (necesarias para MedicamentosActivity)
    public String getUrlProspecto() { return urlProspecto; }
    public void setUrlProspecto(String urlProspecto) { this.urlProspecto = emptyToNull(urlProspecto); }

    public String getUrlFichaTecnica() { return urlFichaTecnica; }
    public void setUrlFichaTecnica(String urlFichaTecnica) { this.urlFichaTecnica = emptyToNull(urlFichaTecnica); }

    // Helpers
    private static String safe(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
