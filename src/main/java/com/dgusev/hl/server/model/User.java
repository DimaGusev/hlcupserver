package com.dgusev.hl.server.model;

/**
 * Created by dgusev on 11.08.2017.
 */
public class User {

    public Integer id;

    public Long birthDate;

    public String gender;

    public String email;

    public String firstName;

    public String lastName;

    public int age;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", birthDate=" + birthDate +
                ", gender='" + gender + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
