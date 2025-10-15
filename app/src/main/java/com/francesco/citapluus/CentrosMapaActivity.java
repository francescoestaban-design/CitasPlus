package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Retrofit – Nearby Search (Places Web Service)
import com.francesco.citapluus.net.places.PlacesNearbyResponse;
import com.francesco.citapluus.net.places.PlacesService;
import com.francesco.citapluus.net.places.RetrofitPlaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CentrosMapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ===== Config =====
    private static final int RADIO_NEARBY_M = 2000;          // centros: 2 km
    private static final int RADIO_NEARBY_FARM_M = 7000;     // farmacias: 7 km (más resultados)
    private static final double RADIO_METROS = 4000;         // abrir el más cercano si cae en rango
    private static final int MAX_SUGERENCIAS = 12;           // Autocomplete
    private static final float MIN_ZOOM_DELTA = 0.4f;        // umbral de zoom para recargar
    private static final float MIN_RELOAD_METERS = 350f;     // umbral de desplazamiento para recargar

    // ===== Google / Places =====
    private GoogleMap map;
    private FusedLocationProviderClient fused;
    private PlacesClient placesClient;
    private PlacesService placesService; // Retrofit (Places Web Service)

    // Geocoder para deduplicar por CP si algún día lo reactivas
    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();

    // UI
    private View panelFiltros;
    private boolean panelOculto = false;
    private ChipGroup chips;
    private Chip chipHosp, chipFarm, chipFav;
    private FloatingActionButton fabMiUbicacion;
    private MaterialButton buttonGuardar, buttonFavorito;

    // Estado mapa/resultados
    private final List<Marker> marcadores = new ArrayList<>();
    private LatLng lastKnown;
    private Marker seleccion;

    // selección actual (puede venir del SDK, de favoritos o de Nearby web)
    private Place seleccionPlace;                         // SDK
    private FavoritePlace seleccionFav;                   // Favoritos locales
    private PlacesNearbyResponse.Result seleccionWeb;     // Result Web (Nearby)

    // Modo de carga
    private enum Modo { NEARBY, AUTOCOMPLETE, FAVORITOS }
    private Modo modoActual = Modo.NEARBY;

    // Anti-recarga por animaciones
    private long suppressReloadUntil = 0;
    private boolean shouldReload() { return android.os.SystemClock.uptimeMillis() >= suppressReloadUntil; }
    private void suprimirRecargaDurante(long ms) { suppressReloadUntil = android.os.SystemClock.uptimeMillis() + ms; }

    // Autocomplete
    private final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
    private final List<AutocompletePrediction> ultimasPredicciones = new ArrayList<>();
    private ArrayAdapter<String> predAdapter;

    // Debounce
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingLoadTask;
    private boolean isLoadingNearby = false;

    // Autocomplete -> FetchPlace
    private int pendingFetchCount = 0;
    private final List<Place> pendingPlaces = new ArrayList<>();

    // Control de recargas por movimiento
    private boolean lastMoveWasGesture = false;
    private LatLng lastQueryCenter = null;
    private float lastQueryZoom = -1f;

    // ===== Permisos =====
    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            enableMyLocation();
                            moverACoordenadaActual();
                            recargarSegunChip(true);
                        } else {
                            Toast.makeText(this, "Sin permiso de ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });

    // ===== Launchers =====
    private final ActivityResultLauncher<Intent> abrirFavLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    r -> {
                        if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                            double lat = r.getData().getDoubleExtra("lat", 0);
                            double lng = r.getData().getDoubleExtra("lng", 0);
                            String nombre = r.getData().getStringExtra("nombre");
                            String direccion = r.getData().getStringExtra("direccion");

                            LatLng ll = new LatLng(lat, lng);
                            if (map != null) {
                                suprimirRecargaDurante(700);
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f));
                            }

                            limpiarMarcadores();
                            Marker mk = map.addMarker(new MarkerOptions()
                                    .position(ll)
                                    .title(nombre)
                                    .snippet(direccion)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                            if (mk != null) mk.showInfoWindow();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_centros_mapa);

        // Toolbar
        MaterialToolbar topBar = findViewById(R.id.topBar);
        topBar.setNavigationOnClickListener(v -> onBackPressed());
        topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search_place) {
                modoActual = Modo.AUTOCOMPLETE;
                abrirBuscadorFiltrado();
                return true;
            } else if (item.getItemId() == R.id.action_favorites) {
                Intent i = new Intent(this, FavoritesActivity.class);
                abrirFavLauncher.launch(i);
                return true;
            }
            return false;
        });

        // Places SDK + Location
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        fused = LocationServices.getFusedLocationProviderClient(this);

        // Places Web Service (Nearby) con Retrofit
        placesService = RetrofitPlaces.get().create(PlacesService.class);

        // UI
        panelFiltros   = findViewById(R.id.cardPanelFiltros);
        chips          = findViewById(R.id.chipsFiltro);
        chipHosp       = findViewById(R.id.chipHospitales);
        chipFarm       = findViewById(R.id.chipFarmacias);
        chipFav        = findViewById(R.id.chipFavoritos);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        buttonGuardar  = findViewById(R.id.buttonGuardarCentro);
        buttonFavorito = findViewById(R.id.buttonFavorito);

        SupportMapFragment frag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (frag != null) frag.getMapAsync(this);

        fabMiUbicacion.setOnClickListener(v -> moverACoordenadaActual());

        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (chipFav.isChecked()) {
                modoActual = Modo.FAVORITOS;
                cargarFavoritos();
            } else {
                modoActual = Modo.NEARBY;
                recargarSegunChip(true); // recarga inmediata al cambiar chip
            }
        });

        // Guardar como “mi centro”
        buttonGuardar.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(this);
            if (seleccionPlace != null && seleccionPlace.getLatLng() != null) {
                LatLng ll = seleccionPlace.getLatLng();
                sm.setCentroSalud(seleccionPlace.getName(), seleccionPlace.getAddress(), ll.latitude, ll.longitude);
                Toast.makeText(this, "Centro guardado: " + seleccionPlace.getName(), Toast.LENGTH_SHORT).show();
                finish();
            } else if (seleccionFav != null) {
                sm.setCentroSalud(seleccionFav.nombre, seleccionFav.direccion, seleccionFav.lat, seleccionFav.lng);
                Toast.makeText(this, "Centro guardado: " + seleccionFav.nombre, Toast.LENGTH_SHORT).show();
                finish();
            } else if (seleccionWeb != null) {
                LatLng p = new LatLng(seleccionWeb.geometry.location.lat, seleccionWeb.geometry.location.lng);
                sm.setCentroSalud(seleccionWeb.name,
                        seleccionWeb.vicinity != null ? seleccionWeb.vicinity : "",
                        p.latitude, p.longitude);
                Toast.makeText(this, "Centro guardado: " + seleccionWeb.name, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Añadir / quitar favorito
        buttonFavorito.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(this);

            // Desde SDK Place
            if (seleccionPlace != null && seleccionPlace.getLatLng() != null) {
                String id = seleccionPlace.getId();
                if (sm.isFavorito(id)) {
                    sm.removeFavoritoById(id);
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    LatLng ll = seleccionPlace.getLatLng();
                    String tipo = (chipFarm.isChecked()) ? "FARMACIA" : "CENTRO_SALUD";
                    sm.addFavorito(new FavoritePlace(
                            id, seleccionPlace.getName(), seleccionPlace.getAddress(),
                            ll.latitude, ll.longitude, tipo
                    ));
                    Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show();
                }
                actualizarTextoBotonFavorito();
                return;
            }

            // Desde Nearby (web)
            if (seleccionWeb != null) {
                String id = seleccionWeb.place_id;
                if (sm.isFavorito(id)) {
                    sm.removeFavoritoById(id);
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    String tipo = (chipFarm.isChecked()) ? "FARMACIA" : "CENTRO_SALUD";
                    sm.addFavorito(new FavoritePlace(
                            id,
                            seleccionWeb.name,
                            seleccionWeb.vicinity != null ? seleccionWeb.vicinity : "",
                            seleccionWeb.geometry.location.lat,
                            seleccionWeb.geometry.location.lng,
                            tipo
                    ));
                    Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show();
                }
                actualizarTextoBotonFavorito();
                return;
            }

            // Desde favoritos
            if (seleccionFav != null) {
                if (sm.isFavorito(seleccionFav.id)) {
                    sm.removeFavoritoById(seleccionFav.id);
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    if (chipFav.isChecked()) cargarFavoritos();
                    seleccionFav = null;
                    seleccion = null;
                }
                actualizarTextoBotonFavorito();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // Ocultar/mostrar panel en gesto
        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                lastMoveWasGesture = true;
                ocultarPanel(true);
            }
        });

        map.setOnCameraIdleListener(() -> {
            ocultarPanel(false);

            if (!shouldReload()) return;
            if (modoActual == Modo.FAVORITOS) return;

            // Sólo recargar si el usuario realmente se movió lo suficiente
            LatLng now = map.getCameraPosition().target;
            float zoomNow = map.getCameraPosition().zoom;

            boolean debe = false;
            if (lastQueryCenter == null) {
                debe = true;
            } else {
                float[] d = new float[1];
                android.location.Location.distanceBetween(
                        lastQueryCenter.latitude, lastQueryCenter.longitude,
                        now.latitude, now.longitude, d);
                float dz = Math.abs(zoomNow - lastQueryZoom);
                if (d[0] >= MIN_RELOAD_METERS || dz >= MIN_ZOOM_DELTA) debe = true;
            }

            if (debe && lastMoveWasGesture) {
                recargarSegunChip(false);
                lastQueryCenter = now;
                lastQueryZoom = zoomNow;
            }

            lastMoveWasGesture = false;
        });

        map.setOnMarkerClickListener(marker -> {
            // Evita que un pequeño recentrado dispare recarga
            suprimirRecargaDurante(900);

            seleccion = marker;
            seleccion.showInfoWindow();
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);

            // Reset selección
            seleccionPlace = null;
            seleccionFav = null;
            seleccionWeb  = null;

            Object tag = marker.getTag();
            if (tag instanceof Place) {
                seleccionPlace = (Place) tag;
            } else if (tag instanceof FavoritePlace) {
                seleccionFav = (FavoritePlace) tag;
            } else if (tag instanceof PlacesNearbyResponse.Result) {
                seleccionWeb = (PlacesNearbyResponse.Result) tag;
            }

            actualizarTextoBotonFavorito();
            return false; // comportamiento por defecto del InfoWindow
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            moverACoordenadaActual();
            recargarSegunChip(true);
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // ===== Nearby (recomendaciones automáticas) =====
    private void recargarSegunChip(boolean inmediata) {
        if (map == null) return;

        if (chipFav.isChecked()) {
            modoActual = Modo.FAVORITOS;
            cargarFavoritos();
            return;
        }

        modoActual = Modo.NEARBY;

        LatLng centro = (map.getCameraPosition() != null)
                ? map.getCameraPosition().target
                : (lastKnown != null ? lastKnown : new LatLng(40.4168, -3.7038));

        Runnable task = () -> {
            if (chipFarm.isChecked()) {
                // Farmacias
                cargarNearby(centro, "pharmacy", null, BitmapDescriptorFactory.HUE_GREEN);
            } else {
                // Centros de salud (médicos) con palabra clave típica en España
                cargarNearby(centro, "doctor", "centro de salud ambulatorio consultorio",
                        BitmapDescriptorFactory.HUE_RED);
            }
        };

        if (inmediata) {
            if (!isLoadingNearby) task.run();
        } else {
            if (pendingLoadTask != null) handler.removeCallbacks(pendingLoadTask);
            pendingLoadTask = () -> { if (!isLoadingNearby) task.run(); };
            handler.postDelayed(pendingLoadTask, 280);
        }
    }

    private void cargarFavoritos() {
        if (map == null) return;

        limpiarMarcadores();
        buttonGuardar.setEnabled(false);
        buttonFavorito.setEnabled(false);
        buttonFavorito.setText("Añadir a favoritos");

        SessionManager sm = new SessionManager(this);
        List<FavoritePlace> favs = sm.getFavoritos();
        if (favs == null || favs.isEmpty()) {
            // sin toasts ruidosos
            return;
        }

        for (FavoritePlace f : favs) {
            MarkerOptions mo = new MarkerOptions()
                    .position(new LatLng(f.lat, f.lng))
                    .title(f.nombre)
                    .snippet(f.direccion)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            Marker mk = map.addMarker(mo);
            if (mk != null) {
                mk.setTag(f);
                marcadores.add(mk);
            }
        }

        if (!marcadores.isEmpty()) {
            marcadores.get(0).showInfoWindow();
            seleccion = marcadores.get(0);
            Object tag = seleccion.getTag();
            seleccionFav = (tag instanceof FavoritePlace) ? (FavoritePlace) tag : null;
            seleccionPlace = null;
            seleccionWeb = null;
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);
            actualizarTextoBotonFavorito();
        }
    }

    private void cargarNearby(@NonNull LatLng centro,
                              @NonNull String tipo,
                              String keywordOrNull,
                              float hue) {
        if (placesService == null) return;
        isLoadingNearby = true;

        String latlng = centro.latitude + "," + centro.longitude;
        String apiKey = getString(R.string.google_places_web_key); // CLAVE WEB

        int radio = "pharmacy".equals(tipo) ? RADIO_NEARBY_FARM_M : RADIO_NEARBY_M;

        Call<PlacesNearbyResponse> call;
        if ("pharmacy".equals(tipo) || keywordOrNull == null) {
            call = placesService.nearbySearch(latlng, radio, tipo, "es", apiKey);
        } else {
            call = placesService.nearbySearchWithKeyword(latlng, radio, tipo, keywordOrNull, "es", apiKey);
        }

        call.enqueue(new Callback<PlacesNearbyResponse>() {
            @Override
            public void onResponse(Call<PlacesNearbyResponse> c, Response<PlacesNearbyResponse> r) {
                isLoadingNearby = false;

                PlacesNearbyResponse body = r.body();
                if (body == null || body.results == null || body.results.isEmpty()) {
                    // Silencio: no pintamos nada
                    limpiarMarcadores();
                    return;
                }

                limpiarMarcadores();
                List<PlacesNearbyResponse.Result> results = body.results;

                // Pinta resultados
                for (PlacesNearbyResponse.Result res : results) {
                    LatLng p = new LatLng(res.geometry.location.lat, res.geometry.location.lng);
                    String direccion = (res.vicinity != null) ? res.vicinity : "";
                    Marker mk = map.addMarker(new MarkerOptions()
                            .position(p)
                            .title(res.name != null ? res.name : "")
                            .snippet(direccion)
                            .icon(BitmapDescriptorFactory.defaultMarker(hue)));
                    if (mk != null) {
                        mk.setTag(res); // ¡importante para favoritos!
                        marcadores.add(mk);
                    }
                }

                if (!marcadores.isEmpty()) {
                    marcadores.get(0).showInfoWindow();
                }
            }

            @Override
            public void onFailure(Call<PlacesNearbyResponse> call, Throwable t) {
                isLoadingNearby = false;
                // Sólo mostramos errores reales (conectividad, etc.)
                Toast.makeText(CentrosMapaActivity.this, "Error Nearby: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== Autocomplete (flujo manual) =====
    private void cargarLugares() {
        if (map == null) return;

        if (chipFav.isChecked()) {
            limpiarMarcadores();
            cargarFavoritos();
            return;
        }

        LatLng centro = (map.getCameraPosition() != null)
                ? map.getCameraPosition().target
                : (lastKnown != null ? lastKnown : new LatLng(40.4168, -3.7038));

        double delta = 0.22;
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(centro.latitude - delta, centro.longitude - delta),
                new LatLng(centro.latitude + delta, centro.longitude + delta)
        );

        final boolean mostrarCentros = !chipFarm.isChecked();
        final String query = mostrarCentros ? "centro de salud" : "farmacia";

        FindAutocompletePredictionsRequest req = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setCountries(Collections.singletonList("ES"))
                .setTypesFilter(Collections.singletonList(mostrarCentros ? PlaceTypes.HOSPITAL : PlaceTypes.PHARMACY))
                .setLocationBias(bounds)
                .build();

        pendingPlaces.clear();
        pendingFetchCount = 0;

        placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener(resp -> {
                    List<AutocompletePrediction> preds = resp.getAutocompletePredictions();
                    if (preds.isEmpty()) {
                        limpiarMarcadores();
                        return;
                    }

                    int count = Math.min(MAX_SUGERENCIAS, preds.size());
                    pendingFetchCount = count;

                    for (int i = 0; i < count; i++) {
                        AutocompletePrediction p = preds.get(i);
                        FetchPlaceRequest fReq = FetchPlaceRequest.newInstance(
                                p.getPlaceId(),
                                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES)
                        );
                        placesClient.fetchPlace(fReq)
                                .addOnSuccessListener((FetchPlaceResponse r) -> {
                                    Place pl = r.getPlace();
                                    if (pl.getLatLng() != null) {
                                        pendingPlaces.add(pl);
                                    }
                                    onFetchTerminado(centro, mostrarCentros);
                                })
                                .addOnFailureListener(e -> onFetchTerminado(centro, mostrarCentros));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando sugerencias: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void onFetchTerminado(LatLng centro, boolean mostrarCentros) {
        pendingFetchCount--;
        if (pendingFetchCount <= 0) {
            pintarSugerenciasLote(pendingPlaces, mostrarCentros, centro);
        }
    }

    private void pintarSugerenciasLote(List<Place> placesOK, boolean mostrarCentros, LatLng centro) {
        limpiarMarcadores();
        if (placesOK == null || placesOK.isEmpty()) return;

        placesOK.sort((a, b) -> {
            float[] da = new float[1], db = new float[1];
            LatLng la = a.getLatLng(), lb = b.getLatLng();
            android.location.Location.distanceBetween(centro.latitude, centro.longitude, la.latitude, la.longitude, da);
            android.location.Location.distanceBetween(centro.latitude, centro.longitude, lb.latitude, lb.longitude, db);
            return Float.compare(da[0], db[0]);
        });

        float color = mostrarCentros ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN;

        for (Place p : placesOK) {
            Marker mk = map.addMarker(new MarkerOptions()
                    .position(p.getLatLng())
                    .title(p.getName())
                    .snippet(p.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
            if (mk != null) {
                mk.setTag(p);
                marcadores.add(mk);
            }
        }

        if (!marcadores.isEmpty()) {
            Marker nearest = null;
            float best = Float.MAX_VALUE;
            float[] res = new float[1];
            for (Marker m : marcadores) {
                LatLng p = m.getPosition();
                android.location.Location.distanceBetween(centro.latitude, centro.longitude, p.latitude, p.longitude, res);
                if (res[0] < best) {
                    best = res[0];
                    nearest = m;
                }
            }
            if (nearest != null && best <= RADIO_METROS) {
                nearest.showInfoWindow();
                seleccion = nearest;
                Object tag = nearest.getTag();
                seleccionPlace = tag instanceof Place ? (Place) tag : null;
                seleccionFav   = null;
                seleccionWeb   = null;
                buttonGuardar.setEnabled(true);
                buttonFavorito.setEnabled(true);
                actualizarTextoBotonFavorito();
            }
        }
    }

    // ===== Panel show/hide =====
    private void ocultarPanel(boolean ocultar) {
        if (panelFiltros == null) return;
        if (panelOculto == ocultar) return;
        panelOculto = ocultar;

        float toAlpha = ocultar ? 0f : 1f;
        float toTransY = ocultar ? -panelFiltros.getHeight() * 0.35f : 0f;

        panelFiltros.animate().alpha(toAlpha).translationY(toTransY).setDuration(180).start();
        panelFiltros.setClickable(!ocultar);
    }

    // ===== Helpers =====
    private void actualizarTextoBotonFavorito() {
        SessionManager sm = new SessionManager(this);
        String id = null;
        if (seleccionPlace != null) id = seleccionPlace.getId();
        else if (seleccionFav != null) id = seleccionFav.id;
        else if (seleccionWeb != null) id = seleccionWeb.place_id;

        if (id == null) {
            buttonFavorito.setText("Añadir a favoritos");
            buttonFavorito.setEnabled(false);
            return;
        }
        boolean ya = sm.isFavorito(id);
        buttonFavorito.setText(ya ? "Quitar de favoritos" : "Añadir a favoritos");
        buttonFavorito.setEnabled(true);
    }

    private void enableMyLocation() {
        if (map == null) return;
        try { map.setMyLocationEnabled(true); } catch (SecurityException ignore) {}
    }

    private void moverACoordenadaActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        try {
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    moverCamaraA(loc.getLatitude(), loc.getLongitude());
                } else {
                    CancellationTokenSource cts = new CancellationTokenSource();
                    fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                            .addOnSuccessListener(cur -> {
                                if (cur != null) moverCamaraA(cur.getLatitude(), cur.getLongitude());
                            });
                }
            });
        } catch (SecurityException ignored) {}
    }

    private void moverCamaraA(double lat, double lng) {
        lastKnown = new LatLng(lat, lng);
        CameraPosition cp = CameraPosition.builder().target(lastKnown).zoom(15f).build();
        if (map != null) {
            suprimirRecargaDurante(700);
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
        }
    }

    private void limpiarMarcadores() {
        for (Marker m : marcadores) m.remove();
        marcadores.clear();
        seleccion = null;
        seleccionPlace = null;
        seleccionFav = null;
        seleccionWeb = null;
        buttonGuardar.setEnabled(false);
        buttonFavorito.setEnabled(false);
        buttonFavorito.setText("Añadir a favoritos");
    }

    private void limpiarYMarcarSeleccion(Place place, LatLng ll) {
        limpiarMarcadores();
        Marker mk = map.addMarker(new MarkerOptions()
                .position(ll)
                .title(place.getName())
                .snippet(place.getAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        if (mk != null) {
            mk.setTag(place);
            mk.showInfoWindow();
            marcadores.add(mk);
            seleccion = mk;
            seleccionPlace = place;
            seleccionFav = null;
            seleccionWeb = null;
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);
            actualizarTextoBotonFavorito();
        }
    }

    // ===== Buscador programático =====
    private void abrirBuscadorFiltrado() {
        final android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        final EditText input = new EditText(this);
        input.setHint("Buscar centro de salud o farmacia…");
        input.setSingleLine(true);
        root.addView(input, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        final ListView list = new ListView(this);
        root.addView(list, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        predAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(predAdapter);

        final androidx.appcompat.app.AlertDialog dlg =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Buscar")
                        .setView(root)
                        .setNegativeButton("Cerrar", (d, w) -> d.dismiss())
                        .create();

        input.addTextChangedListener(new TextWatcher() {
            private long last = 0;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                final String q = s.toString().trim();
                long now = System.currentTimeMillis();
                last = now;
                list.postDelayed(() -> { if (now == last) solicitarPrediccionesFiltradas(q); }, 250);
            }
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= ultimasPredicciones.size()) return;
            AutocompletePrediction sel = ultimasPredicciones.get(position);
            fetchPlaceYMarcar(sel.getPlaceId());
            dlg.dismiss();
        });

        dlg.setOnShowListener(d -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        dlg.show();
    }

    private void solicitarPrediccionesFiltradas(String query) {
        if (query.length() < 2) {
            ultimasPredicciones.clear();
            if (predAdapter != null) {
                predAdapter.clear();
                predAdapter.notifyDataSetChanged();
            }
            return;
        }

        LatLng centro = (map != null && map.getCameraPosition() != null)
                ? map.getCameraPosition().target
                : (lastKnown != null ? lastKnown : new LatLng(40.4168, -3.7038));
        double delta = 0.20;

        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(centro.latitude - delta, centro.longitude - delta),
                new LatLng(centro.latitude + delta, centro.longitude + delta)
        );

        List<String> tipos = new ArrayList<>();
        String qLower = query.toLowerCase();
        if (qLower.contains("farm") || (chipFarm != null && chipFarm.isChecked())) {
            tipos.add(PlaceTypes.PHARMACY);
        } else {
            tipos.add(PlaceTypes.HOSPITAL);
        }

        FindAutocompletePredictionsRequest req = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setCountries(Collections.singletonList("ES"))
                .setTypesFilter(tipos)
                .setLocationBias(bounds)
                .build();

        placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener(resp -> {
                    ultimasPredicciones.clear();
                    ultimasPredicciones.addAll(resp.getAutocompletePredictions());

                    List<String> labels = new ArrayList<>();
                    for (AutocompletePrediction p : ultimasPredicciones) {
                        String primary = p.getPrimaryText(null).toString();
                        String secondary = p.getSecondaryText(null).toString();
                        labels.add(secondary.isEmpty() ? primary : (primary + " - " + secondary));
                    }
                    if (predAdapter != null) {
                        predAdapter.clear();
                        predAdapter.addAll(labels);
                        predAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Predicciones fallaron: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchPlaceYMarcar(String placeId) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        FetchPlaceRequest req = FetchPlaceRequest.newInstance(placeId, fields);

        placesClient.fetchPlace(req)
                .addOnSuccessListener((FetchPlaceResponse response) -> {
                    Place place = response.getPlace();
                    if (place.getLatLng() == null) {
                        Toast.makeText(this, "Lugar sin coordenadas.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LatLng ll = place.getLatLng();
                    if (map != null) {
                        suprimirRecargaDurante(700);
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f));
                    }
                    limpiarYMarcarSeleccion(place, ll);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "No se pudo obtener el lugar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
