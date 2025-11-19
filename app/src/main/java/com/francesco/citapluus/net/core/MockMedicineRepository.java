package com.francesco.citapluus.net.core;

import com.francesco.citapluus.DrugInfo;
import com.francesco.citapluus.net.dto.DrugInfoDto;
import com.francesco.citapluus.net.mappers.DrugMappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MockMedicineRepository implements MedicineRepository {
    private final ApiService api;

    public MockMedicineRepository(ApiService api) {
        this.api = api;
    }

    @Override
    public void search(String query, MedicineRepository.Callback cb) {
        api.searchMedicines(query).enqueue(new retrofit2.Callback<List<DrugInfoDto>>() {
            @Override
            public void onResponse(Call<List<DrugInfoDto>> call, Response<List<DrugInfoDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    cb.onError(new RuntimeException("HTTP " + resp.code()));
                    return;
                }
                List<DrugInfo> out = new ArrayList<>();
                for (DrugInfoDto dto : resp.body()) {
                    DrugInfo x = DrugMappers.fromDto(dto);
                    if (x != null) out.add(x);
                }
                cb.onSuccess(out);
            }

            @Override
            public void onFailure(Call<List<DrugInfoDto>> call, Throwable t) {
                cb.onError(t);
            }
        });
    }

    @Override
    public void suggest(String query, MedicineRepository.SuggestCallback cb) {
        if (query == null || query.trim().isEmpty()) {
            cb.onSuccess(Collections.emptyList());
            return;
        }
        api.suggest(query.trim()).enqueue(new retrofit2.Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    cb.onError(new RuntimeException("HTTP " + resp.code()));
                    return;
                }
                cb.onSuccess(resp.body());
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                cb.onError(t);
            }
        });
    }
}
