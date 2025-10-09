package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CentrosMapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ===== Config =====
    private static final double RADIO_METROS = 8000; // para seleccionar el más cercano entre sugerencias
    private static final int MAX_SUGERENCIAS = 12;   // cuántas predicciones pintamos

    // ===== Google / Places =====
    private GoogleMap map;
    private FusedLocationProviderClient fused;
    private PlacesClient placesClient;

    // ===== UI =====
    private View panelFiltros; // MaterialCardView (id: cardPanelFiltros)
    private boolean panelOculto = false;
    private ChipGroup chips;
    private Chip chipHosp, chipFarm, chipFav;
    private FloatingActionButton fabMiUbicacion;
    private MaterialButton buttonGuardar, buttonFavorito;

    // ===== Estado =====
    private final List<Marker> marcadores = new ArrayList<>();
    private LatLng lastKnown;
    private Marker seleccion;
    private Place seleccionPlace;          // marcador de Places
    private FavoritePlace seleccionFav;    // marcador de favoritos

    // Solo recargar sugerencias si el movimiento fue por gesto del usuario
    private boolean recargarAlSoltarPorGesto = false;

    // ===== Autocomplete programático =====
    private AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
    private final List<AutocompletePrediction> ultimasPredicciones = new ArrayList<>();
    private ArrayAdapter<String> predAdapter;

    // ===== Permisos =====
    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            enableMyLocation();
                            moverACoordenadaActual();
                            cargarLugares(); // primera carga
                        } else {
                            Toast.makeText(this, "Sin permiso de ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });

    // Mantengo el launcher por si vuelves a la UI nativa de Autocomplete
    private final ActivityResultLauncher<Intent> autocompleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Place place = Autocomplete.getPlaceFromIntent(result.getData());
                            if (place != null && place.getLatLng() != null) {
                                LatLng ll = place.getLatLng();
                                if (map != null) {
                                    recargarAlSoltarPorGesto = false; // ← importante
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f));
                                }
                                limpiarYMarcarSeleccion(place, ll);
                            }
                        }
                    });

    // ===== Launcher para abrir lista de favoritos (debe ser CAMPO, no dentro de onCreate) =====
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
                                recargarAlSoltarPorGesto = false;
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
                abrirBuscadorFiltrado();
                return true;
            } else if (item.getItemId() == R.id.action_favorites) {
                Intent i = new Intent(this, FavoritesActivity.class);
                abrirFavLauncher.launch(i);
                return true;
            }
            return false;
        });

        // Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        fused = LocationServices.getFusedLocationProviderClient(this);

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
            if (chipFav.isChecked()) cargarFavoritos();
            else cargarLugares();
        });

        // Guardar como "mi centro"
        buttonGuardar.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(this);
            if (seleccionPlace != null && seleccionPlace.getLatLng() != null) {
                LatLng ll = seleccionPlace.getLatLng();
                sm.setCentroSalud(seleccionPlace.getName(), seleccionPlace.getAddress(),
                        ll.latitude, ll.longitude);
                Toast.makeText(this, "Centro guardado: " + seleccionPlace.getName(), Toast.LENGTH_SHORT).show();
                finish();
            } else if (seleccionFav != null) {
                sm.setCentroSalud(seleccionFav.nombre, seleccionFav.direccion,
                        seleccionFav.lat, seleccionFav.lng);
                Toast.makeText(this, "Centro guardado: " + seleccionFav.nombre, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Añadir / quitar favorito
        buttonFavorito.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(this);
            if (seleccionPlace != null && seleccionPlace.getLatLng() != null) {
                String id = seleccionPlace.getId();
                if (sm.isFavorito(id)) {
                    sm.removeFavoritoById(id);
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    LatLng ll = seleccionPlace.getLatLng();
                    String tipo = chipHosp.isChecked() ? "HOSPITAL" : "FARMACIA";
                    sm.addFavorito(new FavoritePlace(
                            id,
                            seleccionPlace.getName(),
                            seleccionPlace.getAddress(),
                            ll.latitude,
                            ll.longitude,
                            tipo
                    ));
                    Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show();
                }
                actualizarTextoBotonFavorito();
            } else if (seleccionFav != null) {
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

        map.setOnMarkerClickListener(marker -> {
            seleccion = marker;
            seleccion.showInfoWindow();
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);

            // reset selección
            seleccionPlace = null;
            seleccionFav = null;

            Object tag = marker.getTag();
            if (tag instanceof Place)      seleccionPlace = (Place) tag;
            else if (tag instanceof FavoritePlace) seleccionFav = (FavoritePlace) tag;

            actualizarTextoBotonFavorito();
            return false;
        });

        // Oculta panel al mover; solo recarga si el movimiento fue por gesto
        map.setOnCameraMoveStartedListener(reason -> {
            recargarAlSoltarPorGesto =
                    (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE);
            ocultarPanel(true);
        });
        map.setOnCameraIdleListener(() -> {
            lastKnown = map.getCameraPosition().target;
            if (recargarAlSoltarPorGesto && !chipFav.isChecked()) {
                cargarLugares();
            }
            recargarAlSoltarPorGesto = false; // reset
            ocultarPanel(false);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            moverACoordenadaActual();
            cargarLugares();
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // ===== Panel show/hide =====
    private void ocultarPanel(boolean ocultar) {
        if (panelFiltros == null) return;
        if (panelOculto == ocultar) return;
        panelOculto = ocultar;

        float toAlpha = ocultar ? 0f : 1f;
        float toTransY = ocultar ? -panelFiltros.getHeight() * 0.35f : 0f;

        panelFiltros.animate()
                .alpha(toAlpha)
                .translationY(toTransY)
                .setDuration(180)
                .start();
        panelFiltros.setClickable(!ocultar);
    }

    // ===== Helpers =====
    private void actualizarTextoBotonFavorito() {
        SessionManager sm = new SessionManager(this);
        String id = null;
        if (seleccionPlace != null) id = seleccionPlace.getId();
        else if (seleccionFav != null) id = seleccionFav.id;

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
            recargarAlSoltarPorGesto = false; // no recargar tras animación programática
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
        }
    }

    private void limpiarMarcadores() {
        for (Marker m : marcadores) m.remove();
        marcadores.clear();
        seleccion = null;
        seleccionPlace = null;
        seleccionFav = null;
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
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);
            actualizarTextoBotonFavorito();
        }
    }

    // ===== RECOMENDACIONES CERCA DEL PUNTO (ES) =====
    private void cargarLugares() {
        if (map == null) return;
        limpiarMarcadores();

        if (chipFav != null && chipFav.isChecked()) {
            cargarFavoritos();
            return;
        }

        // Centro = cámara si existe; si no, lastKnown; si no, Madrid
        LatLng centro = (map != null) ? map.getCameraPosition().target
                : (lastKnown != null ? lastKnown : new LatLng(40.4168, -3.7038));

        double delta = 0.22; // ~20–25 km
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(centro.latitude - delta, centro.longitude - delta),
                new LatLng(centro.latitude + delta, centro.longitude + delta)
        );

        final boolean mostrarHospitales = chipHosp != null && chipHosp.isChecked();
        final String query = mostrarHospitales ? "hospital" : "farmacia";

        FindAutocompletePredictionsRequest req = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setCountries(Collections.singletonList("ES"))
                .setTypesFilter(Collections.singletonList(
                        mostrarHospitales ? PlaceTypes.HOSPITAL : PlaceTypes.PHARMACY
                ))
                .setLocationBias(bounds)
                .build();

        placesClient.findAutocompletePredictions(req)
                .addOnSuccessListener(resp -> {
                    List<AutocompletePrediction> preds = resp.getAutocompletePredictions();
                    if (preds.isEmpty()) {
                        Toast.makeText(this,
                                mostrarHospitales
                                        ? "No se encontraron centros de salud cerca."
                                        : "No se encontraron farmacias cerca.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int count = Math.min(MAX_SUGERENCIAS, preds.size());
                    final List<Place> placesOK = new ArrayList<>();

                    for (int i = 0; i < count; i++) {
                        AutocompletePrediction p = preds.get(i);
                        FetchPlaceRequest fReq = FetchPlaceRequest.newInstance(
                                p.getPlaceId(),
                                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                        );
                        placesClient.fetchPlace(fReq)
                                .addOnSuccessListener((FetchPlaceResponse r) -> {
                                    Place pl = r.getPlace();
                                    if (pl.getLatLng() != null) placesOK.add(pl);
                                    pintarSugerencias(placesOK, mostrarHospitales, centro);
                                })
                                .addOnFailureListener(e -> { /* ignorar fallos individuales */ });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando sugerencias: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void pintarSugerencias(List<Place> placesOK, boolean mostrarHospitales, LatLng centro) {
        if (placesOK.isEmpty()) return;

        limpiarMarcadores();

        // ordenar por distancia al centro
        placesOK.sort((a, b) -> {
            float[] da = new float[1], db = new float[1];
            LatLng la = a.getLatLng(), lb = b.getLatLng();
            android.location.Location.distanceBetween(centro.latitude, centro.longitude, la.latitude, la.longitude, da);
            android.location.Location.distanceBetween(centro.latitude, centro.longitude, lb.latitude, lb.longitude, db);
            return Float.compare(da[0], db[0]);
        });

        float color = mostrarHospitales
                ? BitmapDescriptorFactory.HUE_RED
                : BitmapDescriptorFactory.HUE_GREEN;

        for (Place p : placesOK) {
            Marker mk = map.addMarker(new MarkerOptions()
                    .position(p.getLatLng())
                    .title(p.getName())
                    .snippet(p.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
            if (mk != null) { mk.setTag(p); marcadores.add(mk); }
        }

        // abrir el más cercano si cae dentro del radio
        if (!marcadores.isEmpty()) {
            Marker nearest = null;
            float best = Float.MAX_VALUE;
            float[] res = new float[1];
            for (Marker m : marcadores) {
                LatLng p = m.getPosition();
                android.location.Location.distanceBetween(
                        centro.latitude, centro.longitude, p.latitude, p.longitude, res);
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
                seleccionFav = null;
                buttonGuardar.setEnabled(true);
                buttonFavorito.setEnabled(true);
                actualizarTextoBotonFavorito();
            }
        }
    }

    /** Pinta favoritos del usuario. */
    private void cargarFavoritos() {
        if (map == null) return;
        limpiarMarcadores();

        SessionManager sm = new SessionManager(this);
        List<FavoritePlace> favs = sm.getFavoritos();
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
        if (marcadores.isEmpty()) {
            Toast.makeText(this,
                    "Aún no tienes favoritos. Toca un marcador y pulsa “Añadir a favoritos”.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ===== Buscador programático (ES + tipos) =====
    private void abrirBuscadorFiltrado() {
        final android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        final EditText input = new EditText(this);
        input.setHint("Buscar hospital o farmacia...");
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
                        .setNegativeButton("Cerrar", (d,w) -> d.dismiss())
                        .create();

        input.addTextChangedListener(new TextWatcher() {
            private long last = 0;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                final String q = s.toString().trim();
                long now = System.currentTimeMillis();
                last = now;
                list.postDelayed(() -> {
                    if (now == last) solicitarPrediccionesFiltradas(q);
                }, 250);
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

        // Centro = cámara si existe; si no, lastKnown; si no, Madrid
        LatLng centro = (map != null) ? map.getCameraPosition().target
                : (lastKnown != null ? lastKnown : new LatLng(40.4168, -3.7038));
        double delta = 0.20; // ~20-25 km

        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(centro.latitude - delta, centro.longitude - delta),
                new LatLng(centro.latitude + delta, centro.longitude + delta)
        );

        // ES + tipos (máximo 1) para no exceder restricciones
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
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
        );
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
                        recargarAlSoltarPorGesto = false; // no recargar tras animación programática
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
