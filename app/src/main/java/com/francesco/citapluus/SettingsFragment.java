package com.francesco.citapluus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_settings, rootKey);

        // Tema (system / light / dark)
        ListPreference themePref = findPreference("pref_tema");
        if (themePref != null) {
            themePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String v = (String) newValue;
                int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                if ("light".equals(v)) mode = AppCompatDelegate.MODE_NIGHT_NO;
                else if ("dark".equals(v)) mode = AppCompatDelegate.MODE_NIGHT_YES;
                AppCompatDelegate.setDefaultNightMode(mode);
                return true;
            });
        }

        // Radio por defecto para Farmacias
        ListPreference farmRadio = findPreference("pref_radio_farmacia");
        if (farmRadio != null) {
            farmRadio.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

        // Radio por defecto para Centros de salud
        ListPreference centerRadio = findPreference("pref_radio_centros");
        if (centerRadio != null) {
            centerRadio.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

        // (Opcional) Url backend real: muestra el valor como summary
        EditTextPreference realUrl = findPreference("pref_real_base_url");
        if (realUrl != null) {
            realUrl.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        }

        // (Opcional) Switch de mock: sin lógica extra (evitamos dependencias con tu App.java)
        SwitchPreferenceCompat mockSwitch = findPreference("pref_mock_enabled");
        // Si en el futuro quieres enlazar con App.isMockEnabled(), lo hacemos aquí.

        // Política de privacidad
        Preference privacy = findPreference("pref_privacy");
        if (privacy != null) {
            privacy.setOnPreferenceClickListener(pref -> {
                Uri uri = Uri.parse("https://tu-dominio.com/politica-de-privacidad");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                try { startActivity(i); } catch (ActivityNotFoundException ignored) {}
                return true;
            });
        }
    }
}
