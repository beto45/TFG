package com.uco.tfg;

public class User {
    private String email;
    private String token;

    public User() {
        // Constructor vacío requerido para Firebase Firestore
    }

    public User(String email) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
