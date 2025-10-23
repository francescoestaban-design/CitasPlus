// SettingsActivity.java
package com.francesco.citapluus;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements androidx.preference.Preference.OnPreferenceChangeListener {

        private SwitchPreferenceCompat mockSwitch;
        private EditTextPreference realUrl;

        @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs_settings, rootKey);

            mockSwitch = findPreference("pref_mock_enabled");
            realUrl    = findPreference("pref_real_base_url");

            boolean mock = App.isMockEnabled();
            if (mockSwitch != null) mockSwitch.setChecked(mock);
            if (realUrl != null)    realUrl.setEnabled(!mock);

            if (mockSwitch != null) mockSwitch.setOnPreferenceChangeListener(this);
            if (realUrl != null)    realUrl.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newVal) {
            if (pref == mockSwitch) {
                boolean enabled = (Boolean) newVal;
                if (realUrl != null) realUrl.setEnabled(!enabled);
                App.setMockEnabled(enabled);       // actualiza App en caliente
                App.notifyNetworkConfigChanged();   // limpia Retrofit para re-crear con nueva baseUrl
            } else if (pref == realUrl) {
                App.setRealBaseUrl((String) newVal);
                App.notifyNetworkConfigChanged();
            }
            return true;
        }
    }
}
