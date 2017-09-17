package com.dgusev.hl.server.model.file;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by dgusev on 18.08.2017.
 */
public class LocationJson {

    private volatile Integer id;
    private volatile String country;
    private volatile String city;
    private volatile String place;
    private volatile Integer distance;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    @JsonSetter("country")
    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    @JsonSetter("city")
    public void setCity(String city) {
        this.city = city;
    }

    public String getPlace() {
        return place;
    }

    @JsonSetter("place")
    public void setPlace(String place) {
        this.place = place;
    }

    public Integer getDistance() {
        return distance;
    }

    @JsonSetter("distance")
    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        LocationJson location = (LocationJson) o;
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
