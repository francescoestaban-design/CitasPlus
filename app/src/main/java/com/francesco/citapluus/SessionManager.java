package com.francesco.citapluus;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Maneja la sesión del usuario y preferencias locales (incluye Favoritos). */
public class SessionManager {

    private static final String PREF_NAME = "UserSession";

    // Claves de sesión
    private static final String KEY_LOGGED_IN      = "isLoggedIn";
    private static final String KEY_DNI            = "dni";
    private static final String KEY_NOMBRE         = "nombre";
    private static final String KEY_APELLIDO_PATERNO = "apellidoPaterno";
    private static final String KEY_APELLIDO_MATERNO = "apellidoMaterno";
    private static final String KEY_TIPO_SANGRE    = "tipoSangre";
    private static final String KEY_CIPA           = "cipa";
    private static final String KEY_CODIGO_POSTAL  = "codigoPostal";
    private static final String KEY_CONTRASENA     = "contrasena";

    // Centro de salud elegido
    private static final String KEY_CENTRO_NOMBRE  = "centro_nombre";
    private static final String KEY_CENTRO_DIR     = "centro_dir";
    private static final String KEY_CENTRO_LAT     = "centro_lat";
    private static final String KEY_CENTRO_LNG     = "centro_lng";

    // Favoritos
    private static final String KEY_FAVORITOS      = "favoritos_json"; // JSONArray de objetos

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ======== Sesión básica ========

    public void createSession(String dni, String nombre, String apPaterno, String apMaterno,
                              String tipoSangre, String cipa, String codigoPostal, String contrasena) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_DNI, dni);
        editor.putString(KEY_NOMBRE, nombre);
        editor.putString(KEY_APELLIDO_PATERNO, apPaterno);
        editor.putString(KEY_APELLIDO_MATERNO, apMaterno);
        editor.putString(KEY_TIPO_SANGRE, tipoSangre);
        editor.putString(KEY_CIPA, cipa);
        editor.putString(KEY_CODIGO_POSTAL, codigoPostal);
        editor.putString(KEY_CONTRASENA, contrasena);
        editor.apply();
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_LOGGED_IN, false); }

    public String getNombreCompleto() {
        String nombre = prefs.getString(KEY_NOMBRE, "");
        String apPat  = prefs.getString(KEY_APELLIDO_PATERNO, "");
        String apMat  = prefs.getString(KEY_APELLIDO_MATERNO, "");
        return (nombre + " " + apPat + " " + apMat).trim();
    }

    public String getDNI()          { return prefs.getString(KEY_DNI, ""); }
    public String getCIPA()         { return prefs.getString(KEY_CIPA, ""); }
    public String getCodigoPostal() { return prefs.getString(KEY_CODIGO_POSTAL, ""); }
    public String getTipoSangre()   { return prefs.getString(KEY_TIPO_SANGRE, ""); }
    public String getContrasena()   { return prefs.getString(KEY_CONTRASENA, ""); }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    // ======== Centro de salud elegido ========

    public void setCentroSalud(String nombre, String direccion, double lat, double lng) {
        editor.putString(KEY_CENTRO_NOMBRE, nombre);
        editor.putString(KEY_CENTRO_DIR, direccion);
        editor.putFloat(KEY_CENTRO_LAT, (float) lat);
        editor.putFloat(KEY_CENTRO_LNG, (float) lng);
        editor.apply();
    }

    public String getCentroNombre()    { return prefs.getString(KEY_CENTRO_NOMBRE, ""); }
    public String getCentroDireccion() { return prefs.getString(KEY_CENTRO_DIR, ""); }
    public double getCentroLat()       { return prefs.getFloat(KEY_CENTRO_LAT, 0f); }
    public double getCentroLng()       { return prefs.getFloat(KEY_CENTRO_LNG, 0f); }

    // ======== Favoritos (persistidos en JSON) ========

    public List<FavoritePlace> getFavoritos() {
        List<FavoritePlace> out = new ArrayList<>();
        String json = prefs.getString(KEY_FAVORITOS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                FavoritePlace f = new FavoritePlace(
                        o.optString("id", ""),
                        o.optString("nombre", ""),
                        o.optString("direccion", ""),
                        o.optDouble("lat", 0),
                        o.optDouble("lng", 0),
                        o.optString("tipo", "")
                );
                out.add(f);
            }
        } catch (JSONException ignore) {}
        return out;
    }

    public void addFavorito(FavoritePlace f) {
        if (f == null || f.id == null) return;
        List<FavoritePlace> list = getFavoritos();

        // evitar duplicados por id
        for (FavoritePlace x : list) {
            if (f.id.equals(x.id)) {
                return; // ya existe
            }
        }
        list.add(f);
        saveFavoritos(list);
    }

    public void removeFavoritoById(String id) {
        if (id == null) return;
        List<FavoritePlace> list = getFavoritos();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (id.equals(list.get(i).id)) list.remove(i);
        }
        saveFavoritos(list);
    }

    public boolean isFavorito(String id) {
        if (id == null) return false;
        for (FavoritePlace f : getFavoritos()) {
            if (id.equals(f.id)) return true;
        }
        return false;
    }
    public String getCentroResumen() {
        String nombre = getCentroNombre();
        String dir    = getCentroDireccion();
        if (nombre == null) nombre = "";
        if (dir == null) dir = "";
        if (nombre.isEmpty() && dir.isEmpty()) return "";
        if (dir.isEmpty()) return nombre;
        return nombre + " — " + dir;
    }

    private void saveFavoritos(List<FavoritePlace> list) {
        JSONArray arr = new JSONArray();
        for (FavoritePlace f : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", f.id);
                o.put("nombre", f.nombre);
                o.put("direccion", f.direccion);
                o.put("lat", f.lat);
                o.put("lng", f.lng);
                o.put("tipo", f.tipo);
            } catch (JSONException ignore) {}
            arr.put(o);
        }
        editor.putString(KEY_FAVORITOS, arr.toString());
        editor.apply();
    }
}
