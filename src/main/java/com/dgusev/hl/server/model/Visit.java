package com.dgusev.hl.server.model;

/**
 * Created by dgusev on 11.08.2017.
 */

public class Visit {

    public int id = -1;
    public int user = -1;
    public int location = -1;
    public long visitedAt = Long.MIN_VALUE;
    public byte mark = -1;

    public byte getMark() {
        return mark;
    }

    @Override
    public boolean equals(Object o) {
        Visit visit = (Visit) o;
        return id == visit.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
