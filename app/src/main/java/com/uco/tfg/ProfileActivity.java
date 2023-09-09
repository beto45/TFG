package com.uco.tfg;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 123;

    private FirebaseFirestore firestore;
    private String currentUserID;
    private EditText editTextUsuarioVinculado;
    private ListView listViewUsuariosVinculados;
    private ArrayAdapter<String> usuariosVinculadosAdapter;
    private List<String> usuariosVinculados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        firestore = FirebaseFirestore.getInstance();
        editTextUsuarioVinculado = findViewById(R.id.editTextUsuarioVinculado);
        Button vincularButton = findViewById(R.id.vincularButton);
        Button eliminarButton = findViewById(R.id.eliminarButton);

        listViewUsuariosVinculados = findViewById(R.id.listViewUsuariosVinculados);

        usuariosVinculadosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listViewUsuariosVinculados.setAdapter(usuariosVinculadosAdapter);

        vincularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuarioVinculado = editTextUsuarioVinculado.getText().toString().trim();

                if (!usuarioVinculado.isEmpty()) {
                    // Consultar el usuario vinculado en la base de datos
                    firestore.collection("usuarios")
                            .whereEqualTo("email", usuarioVinculado)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        if (!task.getResult().isEmpty()) {
                                            // Usuario vinculado encontrado
                                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                            String usuarioVinculadoId = document.getId();
                                            actualizarUsuarioVinculado(usuarioVinculadoId);
                                        } else {
                                            Toast.makeText(ProfileActivity.this, "El usuario vinculado no existe", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Error al consultar la base de datos", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(ProfileActivity.this, "Ingrese un usuario vinculado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        eliminarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuarioVinculado = editTextUsuarioVinculado.getText().toString().trim();

                if (!usuarioVinculado.isEmpty()) {
                    eliminarUsuarioVinculado(usuarioVinculado);
                } else {
                    Toast.makeText(ProfileActivity.this, "Ingrese un usuario vinculado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cargarUsuariosVinculados();
    }

    private void cargarUsuariosVinculados() {
        firestore.collection("usuarios")
                .document(currentUserID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(ProfileActivity.this, "Error al cargar los usuarios vinculados", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        usuariosVinculados = (List<String>) documentSnapshot.get("emailsUsuariosVinculados");
                        usuariosVinculadosAdapter.clear();
                        if (usuariosVinculados != null) {
                            usuariosVinculadosAdapter.addAll(usuariosVinculados);
                        }
                        usuariosVinculadosAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void actualizarUsuarioVinculado(String usuarioVinculadoId) {
        if (usuarioVinculadoId.equals(currentUserID)) {
            Toast.makeText(ProfileActivity.this, "No puedes agregar tu propio ID como usuario vinculado", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("usuarios")
                .document(currentUserID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> usuarioVinculadoMap = new HashMap<>();
                            usuarioVinculadoMap.put("id", usuarioVinculadoId);

                            List<Map<String, Object>> usuariosVinculados = (List<Map<String, Object>>) document.get("usuariosVinculados");
                            if (usuariosVinculados != null && usuariosVinculados.contains(usuarioVinculadoMap)) {
                                Toast.makeText(ProfileActivity.this, "El usuario vinculado ya existe en la lista", Toast.LENGTH_SHORT).show();
                                return;
                            }


                            firestore.collection("usuarios")
                                        .document(usuarioVinculadoId)
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                DocumentSnapshot vinculadoDocument = task1.getResult();
                                                if (vinculadoDocument != null && vinculadoDocument.exists()) {
                                                    firestore.collection("usuarios")
                                                            .document(currentUserID)
                                                            .update("usuariosVinculados", FieldValue.arrayUnion(usuarioVinculadoMap))
                                                            .addOnCompleteListener(task2 -> {
                                                                if (task2.isSuccessful()) {
                                                                    Toast.makeText(ProfileActivity.this, "Usuario vinculado actualizado", Toast.LENGTH_SHORT).show();
                                                                    String email = vinculadoDocument.getString("email");
                                                                    String token = vinculadoDocument.getString("token");
                                                                    if (email != null && !email.isEmpty() && token != null && !token.isEmpty()) {
                                                                        firestore.collection("usuarios")
                                                                                .document(currentUserID)
                                                                                .update("emailsUsuariosVinculados", FieldValue.arrayUnion(email))
                                                                                .addOnCompleteListener(task3 -> {
                                                                                    if (task3.isSuccessful()) {
                                                                                        Toast.makeText(ProfileActivity.this, "Email de usuario vinculado agregado", Toast.LENGTH_SHORT).show();
                                                                                        firestore.collection("usuarios")
                                                                                                .document(currentUserID)
                                                                                                .update("tokensUsuariosVinculados", FieldValue.arrayUnion(token))
                                                                                                .addOnCompleteListener(task4 -> {
                                                                                                    if (task4.isSuccessful()) {
                                                                                                        Toast.makeText(ProfileActivity.this, "Token de usuario vinculado agregado", Toast.LENGTH_SHORT).show();
                                                                                                    } else {
                                                                                                        Toast.makeText(ProfileActivity.this, "Error al agregar el token de usuario vinculado", Toast.LENGTH_SHORT).show();
                                                                                                    }
                                                                                                });
                                                                                    } else {
                                                                                        Toast.makeText(ProfileActivity.this, "Error al agregar el email de usuario vinculado", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                    } else {
                                                                        Toast.makeText(ProfileActivity.this, "El usuario vinculado no tiene un email o token válido", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                } else {
                                                                    Toast.makeText(ProfileActivity.this, "Error al actualizar el usuario vinculado", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(ProfileActivity.this, "El usuario vinculado no existe", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(ProfileActivity.this, "Error al consultar la base de datos", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error al consultar la base de datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void eliminarUsuarioVinculado(String usuarioVinculado) {
        // Consultar el usuario vinculado en la base de datos
        firestore.collection("usuarios")
                .whereEqualTo("email", usuarioVinculado)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Usuario vinculado encontrado
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                String usuarioVinculadoId = document.getId();
                                actualizarUsuarioVinculado(usuarioVinculadoId);
                                eliminarEmailUsuarioVinculado(usuarioVinculado);
                                eliminarTokenUsuarioVinculado(usuarioVinculado);
                                eliminarUsuarioVinculadoDeLista(usuarioVinculadoId);
                            } else {
                                Toast.makeText(ProfileActivity.this, "El usuario vinculado no existe", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "Error al consultar la base de datos", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void eliminarEmailUsuarioVinculado(String emailUsuarioVinculado) {
        firestore.collection("usuarios")
                .document(currentUserID)
                .update("emailsUsuariosVinculados", FieldValue.arrayRemove(emailUsuarioVinculado))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Email de usuario vinculado eliminado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Error al eliminar el email de usuario vinculado", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void eliminarTokenUsuarioVinculado(String emailUsuarioVinculado) {
        firestore.collection("usuarios")
                .whereEqualTo("email", emailUsuarioVinculado)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                String usuarioVinculadoId = document.getId();
                                List<String> tokensUsuariosVinculados = (List<String>) document.get("tokensUsuariosVinculados");
                                if (tokensUsuariosVinculados != null) {
                                    tokensUsuariosVinculados.forEach(token -> {
                                        firestore.collection("usuarios")
                                                .document(currentUserID)
                                                .update("tokensUsuariosVinculados", FieldValue.arrayRemove(token))
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(ProfileActivity.this, "Token de usuario vinculado eliminado", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(ProfileActivity.this, "Error al eliminar el token de usuario vinculado", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    });
                                }
                            }
                        }
                    }
                });
    }

    private void eliminarUsuarioVinculadoDeLista(String usuarioVinculadoId) {
        if (usuariosVinculados != null && usuariosVinculados.contains(usuarioVinculadoId)) {
            usuariosVinculados.remove(usuarioVinculadoId);
            firestore.collection("usuarios")
                    .document(currentUserID)
                    .update("usuariosVinculados", usuariosVinculados)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Usuario vinculado eliminado de la lista", Toast.LENGTH_SHORT).show();

                                // Actualizar la lista de usuarios vinculados en el adaptador
                                usuariosVinculadosAdapter.clear();
                                usuariosVinculadosAdapter.addAll(usuariosVinculados);
                                usuariosVinculadosAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Error al eliminar el usuario vinculado de la lista", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
