package com.uco.tfg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class RegistrarseActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText correo;
    private EditText password;
    private EditText passwordConfirmation;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        correo = findViewById(R.id.Correo);
        password = findViewById(R.id.Password);
        passwordConfirmation = findViewById(R.id.PasswordConfirmation);
    }

    public void registrarUsuario(View view) {
        if (password.getText().toString().equals(passwordConfirmation.getText().toString())) {
            mAuth.createUserWithEmailAndPassword(correo.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            obtenerYGuardarTokenFCM(user);
                        } else {
                            Toast.makeText(getApplicationContext(), "Error al crear usuario", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
        }
    }

    private void obtenerYGuardarTokenFCM(FirebaseUser user) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        guardarTokenEnFirestore(user.getUid(), correo.getText().toString(), token);
                    } else {
                        // No se pudo obtener el token de FCM
                        Exception exception = task.getException();
                        if (exception != null) {
                            // Manejar el error
                        }
                    }
                });
    }

    private void guardarTokenEnFirestore(String userId, String userEmail, String userToken) {
        // Crear un objeto HashMap para guardar los datos del usuario
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", userEmail);
        userData.put("token", userToken);

        // Guardar los datos del usuario en Firestore
        db.collection("usuarios").document(userId)
                .set(userData)
                .addOnSuccessListener(documentReference -> {
                    // El token se ha guardado correctamente en Firestore
                    Toast.makeText(getApplicationContext(), "Usuario creado correctamente", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    // Ocurrió un error al guardar el token en Firestore
                });
    }

}



