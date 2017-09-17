package com.dgusev.hl.server.model.file;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by dgusev on 18.08.2017.
 */
public class VisitJson {

    private volatile Integer id;
    private volatile Integer user;
    private volatile Integer location;
    private volatile Long visitedAt;
    private volatile Integer mark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUser() {
        return user;
    }

    @JsonSetter("user")
    public void setUser(Integer user) {
        this.user = user;
    }

    public Integer getLocation() {
        return location;
    }

    @JsonSetter("location")
    public void setLocation(Integer location) {
        this.location = location;
    }

    public Long getVisitedAt() {
        return visitedAt;
    }

    @JsonSetter("visited_at")
    public void setVisitedAt(Long visitedAt) {
        this.visitedAt = visitedAt;
    }

    public Integer getMark() {
        return mark;
    }

    @JsonSetter("mark")
    public void setMark(Integer mark) {
        this.mark = mark;
    }

    @Override
    public boolean equals(Object o) {
        VisitJson visit = (VisitJson) o;
        return id.equals(visit.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
