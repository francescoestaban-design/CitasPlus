package com.francesco.citapluus.ui;

import java.util.ArrayList;
import java.util.List;

public class DrugInfo {
    private String nombre;
    private String paraQueSirve;
    private String comoTomarlo;
    private String advertencias;
    private String efectosSecundarios;
    private List<String> alergenos = new ArrayList<>();

    // URLs opcionales
    private String urlProspecto;     // puede ser null
    private String urlFichaTecnica;  // puede ser null

    // Constructor base (6 params)
    public DrugInfo(String nombre, String paraQueSirve, String comoTomarlo,
                    String advertencias, String efectosSecundarios, List<String> alergenos) {
        this.nombre = nombre;
        this.paraQueSirve = paraQueSirve;
        this.comoTomarlo = comoTomarlo;
        this.advertencias = advertencias;
        this.efectosSecundarios = efectosSecundarios;
        if (alergenos != null) this.alergenos = alergenos;
    }

    // Constructor extendido (8 params) con URLs
    public DrugInfo(String nombre, String paraQueSirve, String comoTomarlo,
                    String advertencias, String efectosSecundarios, List<String> alergenos,
                    String urlProspecto, String urlFichaTecnica) {
        this(nombre, paraQueSirve, comoTomarlo, advertencias, efectosSecundarios, alergenos);
        this.urlProspecto = urlProspecto;
        this.urlFichaTecnica = urlFichaTecnica;
    }

    public String getNombre() { return nombre; }
    public String getParaQueSirve() { return paraQueSirve; }
    public String getComoTomarlo() { return comoTomarlo; }
    public String getAdvertencias() { return advertencias; }
    public String getEfectosSecundarios() { return efectosSecundarios; }

    public List<String> getAlergenos() { return alergenos; }
    public void setAlergenos(List<String> alergenos) {
        this.alergenos = (alergenos != null) ? alergenos : new ArrayList<>();
    }

    public String getUrlProspecto() { return urlProspecto; }
    public void setUrlProspecto(String urlProspecto) { this.urlProspecto = urlProspecto; }

    public String getUrlFichaTecnica() { return urlFichaTecnica; }
    public void setUrlFichaTecnica(String urlFichaTecnica) { this.urlFichaTecnica = urlFichaTecnica; }
}
