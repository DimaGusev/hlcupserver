package com.dgusev.hl.server.model.file;

import java.util.List;

/**
 * Created by dgusev on 13.08.2017.
 */
public class UsersFile {

    private List<UserJson> users;

    public List<UserJson> getUsers() {
        return users;
    }

    public void setUsers(List<UserJson> users) {
        this.users = users;
    }
}
