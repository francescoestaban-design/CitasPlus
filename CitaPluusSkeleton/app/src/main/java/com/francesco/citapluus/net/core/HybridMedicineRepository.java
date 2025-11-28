package com.francesco.citapluus.net.core;

import com.francesco.citapluus.DrugInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Primero intenta en MOCK; si no hay resultados o falla, intenta en CIMA.
 * Útil en debug para mantener tus 3 fármacos + sugerencias reales.
 */
public class HybridMedicineRepository implements MedicineRepository {

    private final MockMedicineRepository mock;
    private final CimaMedicineRepository cima;

    public HybridMedicineRepository(ApiService mockApi, CimaApi cimaApi) {
        this.mock = new MockMedicineRepository(mockApi);
        this.cima  = new CimaMedicineRepository(cimaApi);
    }

    @Override
    public void search(String query, Callback cb) {
        mock.search(query, new Callback() {
            @Override public void onSuccess(List<DrugInfo> items) {
                if (items != null && !items.isEmpty()) { cb.onSuccess(items); return; }
                // fallback a CIMA si mock devuelve vacío
                cima.search(query, cb);
            }
            @Override public void onError(Throwable t) {
                // si mock falla, probamos CIMA
                cima.search(query, cb);
            }
        });
    }

    @Override
    public void suggest(String query, SuggestCallback cb) {
        mock.suggest(query, new SuggestCallback() {
            @Override public void onSuccess(List<String> nombres) {
                if (nombres != null && !nombres.isEmpty()) { cb.onSuccess(nombres); return; }
                // fallback a CIMA si mock no sugiere
                cima.suggest(query, cb);
            }
            @Override public void onError(Throwable t) {
                // si mock falla, probamos CIMA
                cima.suggest(query, cb);
            }
        });
    }
}
