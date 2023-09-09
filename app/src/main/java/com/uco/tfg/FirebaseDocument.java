package com.uco.tfg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FirebaseDocument {
    private String contenido;
    private Date fecha;

    // Constructor
    public FirebaseDocument(String contenido, Date fecha) {
        this.contenido = contenido;
        this.fecha = fecha;
    }

    // Getters
    public String getContenido() {
        return contenido;
    }

    public Date getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
        String formattedDate = dateFormat.format(fecha);

        return "Fecha: " + formattedDate + "\n" +
                "Contenido: " + contenido;
    }
}
