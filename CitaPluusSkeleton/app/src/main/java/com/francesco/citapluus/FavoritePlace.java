package com.francesco.citapluus;

public class FavoritePlace {
    public String id;
    public String nombre;
    public String direccion;
    public double lat;
    public double lng;
    public String tipo;

    public FavoritePlace(String id, String nombre, String direccion,
                         double lat, double lng, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.lat = lat;
        this.lng = lng;
        this.tipo = tipo;
    }

    public FavoritePlace() {}
}
