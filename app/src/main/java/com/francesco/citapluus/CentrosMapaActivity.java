package com.francesco.citapluus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CentrosMapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fused;
    private PlacesClient placesClient;

    private Marker seleccion;
    private Place seleccionPlace;          // marcador de Places
    private FavoritePlace seleccionFav;    // marcador de favoritos

    private ChipGroup chips;
    private Chip chipHosp, chipFarm, chipFav;
    private FloatingActionButton fabMiUbicacion;
    private MaterialButton buttonGuardar, buttonFavorito;

    private final List<Marker> marcadores = new ArrayList<>();
    private LatLng lastKnown;              // última ubicación conocida

    // Launcher para la búsqueda (lupa)
    private final ActivityResultLauncher<Intent> autocompleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Place place = Autocomplete.getPlaceFromIntent(result.getData());
                            if (place != null && place.getLatLng() != null) {
                                LatLng ll = place.getLatLng();
                                if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16f));

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
                        }
                    });

    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            enableMyLocation();
                            moverACoordenadaActual();
                            cargarLugares();
                        } else {
                            Toast.makeText(this, "Sin permiso de ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_centros_mapa);

        // Toolbar con back + listener de la lupa (el menú se declara en el XML)
        MaterialToolbar topBar = findViewById(R.id.topBar);
        topBar.setNavigationOnClickListener(v -> onBackPressed());
        topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search_place) {
                lanzarBusquedaLugar();
                return true;
            }
            return false;
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        fused = LocationServices.getFusedLocationProviderClient(this);

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
            if (tag instanceof Place) {
                seleccionPlace = (Place) tag;
            } else if (tag instanceof FavoritePlace) {
                seleccionFav = (FavoritePlace) tag;
            }
            actualizarTextoBotonFavorito();
            return false;
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
        if (map != null) map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
    }

    /** Lugares cercanos de Places (hospitales o farmacias). */
    private void cargarLugares() {
        if (map == null) return;
        limpiarMarcadores();

        if (chipFav != null && chipFav.isChecked()) {
            cargarFavoritos();
            return;
        }

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS, Place.Field.TYPES
        );
        FindCurrentPlaceRequest req = FindCurrentPlaceRequest.newInstance(fields);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        placesClient.findCurrentPlace(req)
                .addOnSuccessListener(response -> {
                    boolean hospitales = chipHosp.isChecked();
                    for (PlaceLikelihood pl : response.getPlaceLikelihoods()) {
                        Place p = pl.getPlace();
                        if (p.getLatLng() == null || p.getTypes() == null) continue;

                        boolean esValido = hospitales
                                ? (p.getTypes().contains(Place.Type.HOSPITAL)
                                || p.getTypes().contains(Place.Type.DOCTOR)
                                || p.getTypes().contains(Place.Type.HEALTH))
                                : p.getTypes().contains(Place.Type.PHARMACY);

                        if (!esValido) continue;

                        MarkerOptions mo = new MarkerOptions()
                                .position(p.getLatLng())
                                .title(p.getName())
                                .snippet(p.getAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        hospitales ? BitmapDescriptorFactory.HUE_RED
                                                : BitmapDescriptorFactory.HUE_GREEN));

                        Marker mk = map.addMarker(mo);
                        if (mk != null) {
                            mk.setTag(p);
                            marcadores.add(mk);
                        }
                    }
                    if (marcadores.isEmpty()) {
                        Toast.makeText(this, "No se encontraron lugares cercanos para este filtro.", Toast.LENGTH_SHORT).show();
                    } else {
                        seleccionarMasCercanoSiHay(lastKnown);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando lugares: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /** Pinta los favoritos guardados del usuario. */
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
        } else {
            seleccionarMasCercanoSiHay(lastKnown);
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

    /** Selecciona y abre el marcador más cercano a 'miPos'. */
    private void seleccionarMasCercanoSiHay(LatLng miPos) {
        if (miPos == null || marcadores.isEmpty()) return;

        Marker nearest = null;
        float best = Float.MAX_VALUE;
        float[] res = new float[1];

        for (Marker m : marcadores) {
            LatLng p = m.getPosition();
            android.location.Location.distanceBetween(
                    miPos.latitude, miPos.longitude, p.latitude, p.longitude, res);
            if (res[0] < best) {
                best = res[0];
                nearest = m;
            }
        }
        if (nearest != null) {
            nearest.showInfoWindow();
            seleccion = nearest;
            buttonGuardar.setEnabled(true);
            buttonFavorito.setEnabled(true);

            Object tag = nearest.getTag();
            seleccionPlace = null;
            seleccionFav = null;
            if (tag instanceof Place) seleccionPlace = (Place) tag;
            else if (tag instanceof FavoritePlace) seleccionFav = (FavoritePlace) tag;

            actualizarTextoBotonFavorito();
        }
    }

    /** Abre la UI de Autocomplete (lupa). */
    private void lanzarBusquedaLugar() {
        // Campos que queremos del lugar seleccionado
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
        );

        try {
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN,   // ← más estable que OVERLAY
                    fields
            )
                    .setCountries(Arrays.asList("ES","MX","AR","PE","CL","US")) // opcional
                    .build(this);

            autocompleteLauncher.launch(intent);

        } catch (Exception e) {
            // Si algo falla (Play Services/Places no disponible), mostramos por qué
            Toast.makeText(this, "No se pudo abrir la búsqueda: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
