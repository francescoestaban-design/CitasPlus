package com.francesco.citapluus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_settings, rootKey);

        // Tema (system / light / dark)
        ListPreference themePref = findPreference("pref_theme");
        if (themePref != null) {
            // Muestra el valor elegido como summary (equivalente a useSimpleSummaryProvider)
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
