package com.francesco.citapluus.net.core;

import com.francesco.citapluus.net.dto.cima.CimaMedicamentoDetalle;
import com.francesco.citapluus.net.dto.cima.CimaMedicamentoResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CimaApi {

    // ✅ Nuevo: devuelve el objeto raíz con "resultados"
    @GET("medicamentos")
    Call<CimaMedicamentoResponse> search(@Query("nombre") String nombre);

    @GET("medicamento/{nregistro}")
    Call<CimaMedicamentoDetalle> detalle(@Path("nregistro") String nregistro);
}
