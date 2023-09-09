package com.uco.tfg;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class IniciarSesionActivity extends AppCompatActivity {


    private EditText Correo;
    private EditText Password;
    private FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iniciar_sesion);

        Correo = findViewById(R.id.Correo);
        Password = findViewById(R.id.Password);

        mAuth = FirebaseAuth.getInstance();
    }

    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        //updateUI(currentUser);
    }

    private void redirectToMap() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        finish();
    }


    public void iniciarsesion(View view) {
        mAuth.signInWithEmailAndPassword(Correo.getText().toString(), Password.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getApplicationContext(), "Autenticación exitosa",
                                Toast.LENGTH_SHORT).show();

                        // Obtener el token actual del dispositivo
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        String token = task1.getResult();
                                        // Actualizar el campo "token" del documento del usuario
                                        actualizarTokenUsuario(user.getUid(), token);
                                    } else {
                                        // Error al obtener el token
                                        Toast.makeText(getApplicationContext(), "Error al obtener el token",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                        redirectToMap(); // Redirigir al usuario al mapa
                    } else {
                        // Sign in failed
                        Toast.makeText(getApplicationContext(), "Fallo al iniciar sesión",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarTokenUsuario(String userId, String token) {
        // Obtener una referencia al documento del usuario
        DocumentReference usuarioRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId);

        // Crear un mapa con los datos a actualizar
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        // Actualizar el documento del usuario con los nuevos datos
        usuarioRef.update(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token actualizado correctamente"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar el token", e));
    }

}