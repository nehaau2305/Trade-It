package edu.uga.cs.tradeit;

/**
 * User initializes the user table in the database. Since
 * password is already saved by the Firebase database, it
 * is not listed here.
 */
public class User {
    // initialize variables
    private String name;
    private String email;   // NEW
    // default empty constructor
    public User() {
        name = null;
        email = null;
    }
    // constructor
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    // getter & setter methods
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
