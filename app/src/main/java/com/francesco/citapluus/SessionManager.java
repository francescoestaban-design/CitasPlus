package com.francesco.citapluus;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private static final String PREFS = "cita_pluus_prefs";

    // Usuario
    private static final String K_DNI         = "dni";
    private static final String K_NOMBRE      = "nombre";
    private static final String K_AP_PATERNO  = "ap_paterno";
    private static final String K_AP_MATERNO  = "ap_materno";
    private static final String K_TIPO_SANGRE = "tipo_sangre";
    private static final String K_CIPA        = "cipa";
    private static final String K_CP          = "codigo_postal";
    private static final String K_PASS        = "contrasena";
    private static final String K_ALERGIAS    = "alergias"; // NUEVO (opcional)

    // Centro de salud
    private static final String K_CENTRO_NOMBRE = "centro_nombre";
    private static final String K_CENTRO_DIR    = "centro_dir";
    private static final String K_CENTRO_LAT    = "centro_lat";
    private static final String K_CENTRO_LNG    = "centro_lng";

    // Favoritos
    private static final String K_FAVORITOS = "favoritos_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ===== USUARIO =====

    public void createSession(String dni, String nombre, String apPaterno, String apMaterno,
                              String tipoSangre, String cipa, String codigoPostal, String contrasena) {
        sp.edit()
                .putString(K_DNI, dni)
                .putString(K_NOMBRE, nombre)
                .putString(K_AP_PATERNO, apPaterno)
                .putString(K_AP_MATERNO, apMaterno)
                .putString(K_TIPO_SANGRE, tipoSangre)
                .putString(K_CIPA, cipa)
                .putString(K_CP, codigoPostal)
                .putString(K_PASS, contrasena)
                .apply();
    }

    public void logout() { sp.edit().clear().apply(); }

    public String getDNI()             { return sp.getString(K_DNI, ""); }
    public String getContrasena()      { return sp.getString(K_PASS, ""); }
    public String getNombre()          { return sp.getString(K_NOMBRE, ""); }
    public String getApellidoPaterno() { return sp.getString(K_AP_PATERNO, ""); }
    public String getApellidoMaterno() { return sp.getString(K_AP_MATERNO, ""); }
    public String getTipoSangre()      { return sp.getString(K_TIPO_SANGRE, ""); }
    public String getCIPA()            { return sp.getString(K_CIPA, ""); }
    public String getCodigoPostal()    { return sp.getString(K_CP, ""); }

    // Alergias (opcional)
    public void setAlergias(String alergias) {
        // Puede ser null o vacío: se guarda vacío.
        sp.edit().putString(K_ALERGIAS, alergias == null ? "" : alergias.trim()).apply();
    }
    public String getAlergias() {
        return sp.getString(K_ALERGIAS, ""); // cadena vacía si no indicó nada
    }

    public String getNombreCompleto() {
        String full = (getNombre() + " " + getApellidoPaterno() + " " + getApellidoMaterno()).trim();
        return full.replaceAll(" +", " ");
    }

    // ===== CENTRO =====

    public void setCentroSalud(String nombre, String direccion, double lat, double lng) {
        sp.edit()
                .putString(K_CENTRO_NOMBRE, nombre)
                .putString(K_CENTRO_DIR, direccion)
                .putString(K_CENTRO_LAT, String.valueOf(lat))
                .putString(K_CENTRO_LNG, String.valueOf(lng))
                .apply();
    }

    public String getCentroNombre()     { return sp.getString(K_CENTRO_NOMBRE, ""); }
    public String getCentroDireccion()  { return sp.getString(K_CENTRO_DIR, ""); }
    public double getCentroLat()        { return parseDouble(sp.getString(K_CENTRO_LAT, "")); }
    public double getCentroLng()        { return parseDouble(sp.getString(K_CENTRO_LNG, "")); }

    public String getCentroResumen() {
        String n = getCentroNombre(), d = getCentroDireccion();
        if (TextUtils.isEmpty(n) && TextUtils.isEmpty(d)) return "Sin centro seleccionado";
        if (TextUtils.isEmpty(d)) return n;
        if (TextUtils.isEmpty(n)) return d;
        return n + "\n" + d;
    }

    private double parseDouble(String v) {
        if (TextUtils.isEmpty(v)) return 0;
        try { return Double.parseDouble(v); } catch (Exception e) { return 0; }
    }

    // ===== FAVORITOS =====

    public List<FavoritePlace> getFavoritos() {
        String json = sp.getString(K_FAVORITOS, "");
        if (TextUtils.isEmpty(json)) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<FavoritePlace>>(){}.getType();
        try {
            List<FavoritePlace> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveFavoritos(List<FavoritePlace> list) {
        sp.edit().putString(K_FAVORITOS, gson.toJson(list)).apply();
    }

    public boolean isFavorito(String id) {
        if (id == null) return false;
        for (FavoritePlace f : getFavoritos()) if (id.equals(f.id)) return true;
        return false;
    }

    public void addFavorito(FavoritePlace f) {
        if (f == null || f.id == null) return;
        List<FavoritePlace> list = getFavoritos();
        for (FavoritePlace x : list) if (f.id.equals(x.id)) return;
        list.add(f);
        saveFavoritos(list);
    }

    public void removeFavoritoById(String id) {
        if (id == null) return;
        List<FavoritePlace> list = getFavoritos();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id)) { list.remove(i); break; }
        }
        saveFavoritos(list);
    }


    public void setTipoSangre(String tipo) {
        sp.edit().putString(K_TIPO_SANGRE, tipo != null ? tipo.trim() : "").apply();
    }
    public void setkAlergias(String alergias) {
        sp.edit().putString(K_ALERGIAS, alergias != null ? alergias.trim() : "").apply();
    }
    public String getkAlergias() {
        return sp.getString(K_ALERGIAS, "");
    }
    public void setCodigoPostal(String cp) {
        sp.edit().putString(K_CP, cp != null ? cp.trim() : "").apply();
    }

}
