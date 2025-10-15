package com.francesco.citapluus.net.places;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PlacesNearbyResponse {
    public String status;
    @SerializedName("error_message")
    public String error_message;

    public List<Result> results;

    public static class Result {
        public String name;
        public String vicinity;
        public String place_id;
        public Geometry geometry;
        public List<String> types;

        public static class Geometry {
            public Location location;
            public static class Location {
                public double lat;
                public double lng;
            }
        }
    }
}
