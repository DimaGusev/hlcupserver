package com.dgusev.hl.server.model.file;

import java.util.List;

/**
 * Created by dgusev on 13.08.2017.
 */
public class VisitsFile {
    private List<VisitJson> visits;

    public List<VisitJson> getVisits() {
        return visits;
    }

    public void setVisits(List<VisitJson> visits) {
        this.visits = visits;
    }
}
