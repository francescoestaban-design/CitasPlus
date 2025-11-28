package com.francesco.citapluus.net.dto;

import java.util.ArrayList;
import java.util.List;

/** DTO que viene del backend/mock */
public class DrugInfoDto {
    public String nombre;
    public String paraQueSirve;
    public String comoTomarlo;
    public String advertencias;
    public String efectosSecundarios;
    public List<String> alergenos;

    // Opcionales: enlaces a CIMA
    public String urlProspecto;     // puede ser null
    public String urlFichaTecnica;  // puede ser null

    public DrugInfoDto() {}

    public DrugInfoDto(String nombre, String paraQueSirve, String comoTomarlo,
                       String advertencias, String efectosSecundarios, List<String> alergenos) {
        this.nombre = nombre;
        this.paraQueSirve = paraQueSirve;
        this.comoTomarlo = comoTomarlo;
        this.advertencias = advertencias;
        this.efectosSecundarios = efectosSecundarios;
        this.alergenos = (alergenos != null) ? alergenos : new ArrayList<>();
    }
}
