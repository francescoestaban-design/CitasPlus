package com.francesco.citapluus.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.francesco.citapluus.net.core.ApiService;
import com.francesco.citapluus.net.core.RetrofitProvider;
import com.francesco.citapluus.net.dto.AppointmentDto;
import com.francesco.citapluus.net.dto.CreateAppointmentReq;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsRepository {

    public interface Callback1<T> { void onResult(T t, Throwable err); }

    private final ApiService api;

    public AppointmentsRepository(@NonNull Context ctx) {
        this.api = RetrofitProvider.get(ctx.getApplicationContext()).create(ApiService.class);
    }

    public void list(String patientDni, String centerId, String dateISO, Callback1<List<AppointmentDto>> cb) {
        api.listAppointments(patientDni, centerId, dateISO).enqueue(new Callback<List<AppointmentDto>>() {
            @Override public void onResponse(Call<List<AppointmentDto>> call, Response<List<AppointmentDto>> resp) {
                cb.onResult(resp.body(), resp.isSuccessful()? null : new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<List<AppointmentDto>> call, Throwable t) { cb.onResult(null, t); }
        });
    }

    public void create(CreateAppointmentReq req, Callback1<AppointmentDto> cb) {
        api.createAppointment(req).enqueue(new Callback<AppointmentDto>() {
            @Override public void onResponse(Call<AppointmentDto> call, Response<AppointmentDto> resp) {
                if (resp.code() == 409) {
                    cb.onResult(null, new IllegalStateException("CONFLICT_409"));
                    return;
                }
                cb.onResult(resp.body(), resp.isSuccessful()? null : new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<AppointmentDto> call, Throwable t) { cb.onResult(null, t); }
        });
    }

    public void delete(String id, Callback1<Void> cb) {
        api.deleteAppointment(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> resp) {
                cb.onResult(null, resp.isSuccessful()? null : new RuntimeException("HTTP "+resp.code()));
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { cb.onResult(null, t); }
        });
    }
}
