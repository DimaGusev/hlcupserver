package com.dgusev.hl.server.model.file;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by dgusev on 18.08.2017.
 */
public class UserJson {

    private volatile Integer id;

    private volatile Long birthDate;

    private volatile String gender;

    private volatile String email;

    private volatile String firstName;

    private volatile String lastName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getBirthDate() {
        return birthDate;
    }

    @JsonSetter("birth_date")
    public void setBirthDate(Long birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    @JsonSetter("gender")
    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFirstName() {
        return firstName;
    }

    @JsonSetter("first_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @JsonSetter("last_name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
    }

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
        UserJson user = (UserJson) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
