package com.fullstackoasis.gpstrackerservice;

public class LatLngPojo {
    public double getLng() {
        return lng;
    }
    public double getLat() {
        return lat;
    }
    private final double lng;
    private final double lat;

    LatLngPojo(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
    public String toString() {
        return this.lat + ", " + this.lng;
    }
}
