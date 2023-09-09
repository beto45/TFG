package com.uco.tfg;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirebaseDataManager {

    public List<FirebaseDocument> generateSampleData(List<DocumentSnapshot> firebaseDocuments) {
        List<FirebaseDocument> documents = new ArrayList<>();

        for (DocumentSnapshot snapshot : firebaseDocuments) {
            String contenido = snapshot.getString("contenido");
            Date fecha = snapshot.getDate("fecha");

            if (contenido != null && fecha != null) {
                FirebaseDocument document = new FirebaseDocument(contenido, fecha);
                documents.add(document);
            }
        }

        return documents;
    }
}
