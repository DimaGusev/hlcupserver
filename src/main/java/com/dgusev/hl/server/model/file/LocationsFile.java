package com.dgusev.hl.server.model.file;

import java.util.List;

/**
 * Created by dgusev on 13.08.2017.
 */
public class LocationsFile {
    private List<LocationJson> locations;

    public List<LocationJson> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationJson> locations) {
        this.locations = locations;
    }
}
