package com.dgusev.hl.server.model;

/**
 * Created by dgusev on 11.08.2017.
 */
public class Location {

    public Integer id;
    public String country;
    public String city;
    public String place;
    public Integer distance;

    @Override
    public boolean equals(Object o) {
        Location location = (Location) o;
        return id.equals(location.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", place='" + place + '\'' +
                ", distance=" + distance +
                '}';
    }
}
