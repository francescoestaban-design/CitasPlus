package com.francesco.citapluus.net.core;

import com.francesco.citapluus.DrugInfo;
import java.util.List;

public interface MedicineRepository {
    void search(String query, Callback cb);

    void suggest(String query, SuggestCallback cb);

    interface Callback {
        void onSuccess(List<DrugInfo> items); // <-- usa el modelo de UI
        void onError(Throwable t);
    }
    interface SuggestCallback {
        void onSuccess(java.util.List<String> nombres);
        void onError(Throwable t);
    }
}
