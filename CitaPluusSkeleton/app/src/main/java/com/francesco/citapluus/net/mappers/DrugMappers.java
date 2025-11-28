package com.francesco.citapluus.net.mappers;

import com.francesco.citapluus.DrugInfo;                 // UI model
import com.francesco.citapluus.net.dto.DrugInfoDto;      // DTO

import java.util.ArrayList;
import java.util.List;

public final class DrugMappers {
    private DrugMappers(){}

    /** Convierte DTO -> modelo UI */
    public static DrugInfo fromDto(DrugInfoDto dto) {
        if (dto == null) return null;

        List<String> alergs = (dto.alergenos != null) ? dto.alergenos : new ArrayList<>();

        DrugInfo x = new DrugInfo(
                safe(dto.nombre),
                safe(dto.paraQueSirve),
                safe(dto.comoTomarlo),
                safe(dto.advertencias),
                safe(dto.efectosSecundarios),
                alergs
        );

        // Propagar URLs (pueden ser null)
        x.setUrlProspecto(dto.urlProspecto);
        x.setUrlFichaTecnica(dto.urlFichaTecnica);

        return x;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
