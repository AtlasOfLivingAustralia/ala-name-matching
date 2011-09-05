package org.ala.biocache.web;

/**
 * Created by IntelliJ IDEA.
 * User: davejmartin2
 * Date: 25/08/2011
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */

public class UserUpload {
    String name = "my test dataset";
    String uid;
    String api_key ="Venezuela";
    String user= "upload services";
    String email = "David.Martin@csiro.au";
    String firstName = "Dave";
    String lastName = "Martin";
    Integer noOfRecords = 100;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getNoOfRecords() {
        return noOfRecords;
    }

    public void setNoOfRecords(Integer noOfRecords) {
        this.noOfRecords = noOfRecords;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }
}
