package com.francesco.citapluus.net.core;

import com.francesco.citapluus.FavoritePlace;
import com.francesco.citapluus.net.dto.AppointmentDto;
import com.francesco.citapluus.net.dto.CreateAppointmentReq;
import com.francesco.citapluus.net.dto.UserProfile;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ---------- Ping (ya lo usas) ----------
    class PingResp { public String msg; }
    @GET("ping") Call<PingResp> ping();

    // ---------- Favoritos ----------
    @GET("favorites") Call<List<FavoritePlace>> listFavorites();
    @POST("favorites") Call<FavoritePlace> addFavorite(@Body FavoritePlace place);
    @DELETE("favorites/{id}") Call<Void> deleteFavorite(@Path("id") String id);

    // ---------- Perfil ----------
    @GET("me") Call<UserProfile> getProfile();

    /** PATCH parcial. Acepta cualquier subset: tipoSangre, alergias, codigoPostal… */
    @PATCH("me") Call<UserProfile> patchProfile(@Body Map<String, Object> patch);



    @PATCH("me") Call<Void> patchMe(@Body Map<String, String> fields);
    // ---------- Citas ----------
    @GET("appointments")
    Call<List<AppointmentDto>> listAppointments(
            @Query("patient") String patientDni,
            @Query("centerId") String centerId,
            @Query("date") String dateISO // opcional
    );

    @POST("appointments")
    Call<AppointmentDto> createAppointment(@Body CreateAppointmentReq req);

    @DELETE("appointments/{id}")
    Call<Void> deleteAppointment(@Path("id") String id);
}
