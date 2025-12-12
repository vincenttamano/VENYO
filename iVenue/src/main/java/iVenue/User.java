package iVenue;

import java.util.Scanner;

public abstract class User {
    private String username;
    private String password;
    private int UserID;

    public User(String username, String password, int UserID) {
        this.username = username;
        this.password = password;
        this.UserID = UserID;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getUserId() {
        return UserID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserId(int UserID) {
        this.UserID = UserID;
    }
}
