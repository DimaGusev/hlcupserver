package com.dgusev.hl.server.model;

import java.util.List;

/**
 * Created by dgusev on 12.08.2017.
 */
public class VisitSearchResponse {

    private List<VisitResponse> visits;

    public List<VisitResponse> getVisits() {
        return visits;
    }

    public void setVisits(List<VisitResponse> visits) {
        this.visits = visits;
    }
}
