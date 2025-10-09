package com.francesco.citapluus;

import java.io.Serializable;

public class DrugInfo implements Serializable {
    private final String nombre;
    private final String paraQueSirve;
    private final String comoTomarlo;
    private final String advertencias;
    private final String efectosSecundarios;

    public DrugInfo(String nombre, String paraQueSirve, String comoTomarlo,
                    String advertencias, String efectosSecundarios) {
        this.nombre = nombre;
        this.paraQueSirve = paraQueSirve;
        this.comoTomarlo = comoTomarlo;
        this.advertencias = advertencias;
        this.efectosSecundarios = efectosSecundarios;
    }

    public String getNombre() { return nombre; }
    public String getParaQueSirve() { return paraQueSirve; }
    public String getComoTomarlo() { return comoTomarlo; }
    public String getAdvertencias() { return advertencias; }
    public String getEfectosSecundarios() { return efectosSecundarios; }
}
