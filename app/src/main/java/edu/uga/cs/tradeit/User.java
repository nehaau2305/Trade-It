package edu.uga.cs.tradeit;

public class User {
    // fields like key, email, & password are already stored in Firebase since it is set up
    // with Email Password Authentication already
    private String name;

    public User() {
        name = null;
    }

    public User(String name) {
        this.name = name;
    }

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
