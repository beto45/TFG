package com.uco.tfg;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VisualizacionLista extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<FirebaseDocument> adapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizacion_lista);

        // Obtener el ID del usuario actual (usando FirebaseAuth, por ejemplo)
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        listView = findViewById(R.id.listView);

        // Obtener una referencia a la colección en Firebase Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference documentsCollection = db.collection("NotificacionesEnviadas");

        // Filtrar los documentos por el ID del usuario
        Query userNotificationsQuery = documentsCollection.whereEqualTo("userId", userId);

        userNotificationsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<FirebaseDocument> documents = new ArrayList<>();
                for (DocumentSnapshot snapshot : task.getResult()) {
                    String contenido = snapshot.getString("contenido");
                    // Puedes manejar la fecha aquí, dependiendo de cómo esté almacenada en Firestore
                    Date fecha = snapshot.getDate("fecha");

                    if (contenido != null && fecha != null) {
                        FirebaseDocument document = new FirebaseDocument(contenido, fecha);
                        documents.add(document);
                    }
                }

                adapter = new ArrayAdapter<>(VisualizacionLista.this, android.R.layout.simple_list_item_1, documents);
                listView.setAdapter(adapter);
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    // Mostrar un mensaje de error al usuario utilizando un diálogo de alerta
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Error")
                            .setMessage("Ocurrió un error al obtener los documentos. Por favor, inténtalo nuevamente más tarde.");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });
    }
}
