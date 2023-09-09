package com.uco.tfg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final float DEFAULT_ZOOM = 15f;

    private ImageView bolitaView;

    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private CountDownTimer countDownTimer;


    private EditText searchEditText;
    private Button searchButton;
    private int durationInSeconds;
    private TextView contadorTiempo;


    private LatLng destinationLatLng;
    private Polyline currentPolyline;

    // El código inicializa el MapView y el FusedLocationProviderClient.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtenemos la referencia al MapView desde el diseño XML
        mapView = findViewById(R.id.mapView);
        bolitaView = findViewById(R.id.bolitaView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Inicializamos el FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtenemos referencias a los elementos de búsqueda
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        contadorTiempo = findViewById(R.id.contadorTiempo);
        Button helpButton = findViewById(R.id.helpButton);
        Button bottomButton1 = findViewById(R.id.bottomButton1);



        // Se agrega OnClickListener al botón de búsqueda
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locationName = searchEditText.getText().toString();
                if (!locationName.isEmpty()) {
                    // Obtenemos la ubicación ingresada por el usuario
                    searchLocation(locationName);
                }
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Llamar al método para enviar la notificación
                enviarNotificacionConUbicacion();
            }
        });

        bottomButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

    }

    // Cuando el mapa esté listo, verificamos permisos y mostramos la ubicación actual
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Verificamos si se han otorgado los permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Habilitamos la capa de mi ubicación
            googleMap.setMyLocationEnabled(true);
            showCurrentLocation();
        } else {
            // Solicitamos permisos de ubicación
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        setMapClickListener();
    }

    // Obtenemos la ubicación actual utilizando el FusedLocationProviderClient y la mostramos en el mapa.
    private void showCurrentLocation() {
        // Verificamos si se han otorgado los permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Obtenemos la ubicación actual del proveedor de ubicación fusionada
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Obtenemos la latitud y longitud de la ubicación actual
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    // Creamos LatLng object para la ubicación actual
                    LatLng currentLocation = new LatLng(lat, lng);

                    // Nos dirigimos a la ubicación actual
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Al hacer clic en el botón de búsqueda, usamos Geocoder para convertir el nombre de la ubicación en coordenadas de latitud y longitud.
    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (!addressList.isEmpty()) {
                Address address = addressList.get(0);
                double lat = address.getLatitude();
                double lng = address.getLongitude();

                LatLng destination = new LatLng(lat, lng);

                // Mostramos el mensaje de confirmación al usuario
                showConfirmationDialog(destination);
            } else {
                Toast.makeText(this, "No se encontró la ubicación: " + locationName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Muestra un cuadro de diálogo de confirmación para establecer la ruta
    private void showConfirmationDialog(final LatLng destination) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmación");
        builder.setMessage("¿Estás seguro de establecer la ruta hacia esta ubicación?");
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Establecer la ruta hacia la ubicación seleccionada
                setRoute(destination);
                // Iniciar la cuenta regresiva del tiempo estimado de la ruta
                startCountdownTimer(durationInSeconds);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // No hacer nada
            }
        });
        builder.show();
    }

    // Método para iniciar la cuenta regresiva
    private void startCountdownTimer(final int durationInSeconds) {
        // Obtener referencia al TextView del tiempo estimado
        final TextView contadorTiempo = findViewById(R.id.contadorTiempo);

        // Cancelar la cuenta regresiva existente si está en curso
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Iniciar la cuenta regresiva
        countDownTimer = new CountDownTimer(durationInSeconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Calcular minutos y segundos restantes
                long secondsRemaining = millisUntilFinished / 1000;
                long minutes = secondsRemaining / 60;
                long seconds = secondsRemaining % 60;

                // Formatear el tiempo restante (ejemplo: 25:30)
                String formattedTime = String.format("%02d:%02d", minutes, seconds);

                // Mostrar el tiempo restante en el TextView
                contadorTiempo.setText("Tiempo restante: " + formattedTime);
            }

            @Override
            public void onFinish() {
                // La cuenta regresiva ha terminado, se ha llegado al destino
                contadorTiempo.setText("¡Llegaste a tu Destino!");
            }
        };
        countDownTimer.start();
    }


    // Establece la ruta en el mapa hacia la ubicación seleccionada
    private void setRoute(LatLng destination) {
        this.destinationLatLng = destination;

        // Eliminamos todos los marcadores existentes
        googleMap.clear();

        // Mostramos el marcador en la ubicación buscada
        googleMap.addMarker(new MarkerOptions().position(destination).title("Destino"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, DEFAULT_ZOOM));

        // Obtenemos la ubicación actual del proveedor de ubicación fusionada
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Obtenemos la latitud y longitud de la ubicación actual
                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();

                    LatLng origin = new LatLng(currentLat, currentLng);

                    // Obtenemos la ruta desde la ubicación actual hasta la ubicación buscada
                    String url = getDirectionsUrl(origin, destination);
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Construye la URL para la API de Direcciones de Google para obtener la ruta entre la ubicación actual y la ubicación buscada.
    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDestination = "destination=" + destination.latitude + "," + destination.longitude;
        String mode = "mode=walking";
        String alternatives = "alternatives=true";
        String parameters = strOrigin + "&" + strDestination + "&" + mode;
        String output = "json";
        String apiKey = "AIzaSyCIWvaRI_odWXd2xCZ4WvFZNGT-HkvD39o"; //

        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + apiKey;
    }

    // Subclase de AsyncTask, descarga la respuesta JSON de la API de Direcciones de Google.
    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Realiza la descarga de la respuesta JSON de la API de Direcciones de Google.
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                InputStream inputStream = conn.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                response = stringBuilder.toString();
                bufferedReader.close();
                inputStreamReader.close();
                inputStream.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        // Analiza la respuesta JSON y dibuja la ruta en el mapa utilizando PolylineOptions y Polyline.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Parsear la respuesta JSON y dibujar la ruta en el mapa
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray routesArray = jsonObject.getJSONArray("routes");
                if (routesArray.length() > 0) {
                    // Encontramos la ruta más corta
                    JSONObject shortestRoute = routesArray.getJSONObject(0);
                    int shortestDistance = shortestRoute.getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("value");

                    for (int i = 1; i < routesArray.length(); i++) {
                        JSONObject route = routesArray.getJSONObject(i);
                        int distance = route.getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("value");
                        if (distance < shortestDistance) {
                            shortestRoute = route;
                            shortestDistance = distance;
                        }
                    }

                    // Obtener información del tiempo estimado
                    JSONObject legsObject = shortestRoute.getJSONArray("legs").getJSONObject(0);
                    durationInSeconds = legsObject.getJSONObject("duration").getInt("value");

                    // Dibujar la ruta en el mapa
                    JSONObject polylineObject = shortestRoute.getJSONObject("overview_polyline");
                    String encodedPolyline = polylineObject.getString("points");
                    List<LatLng> points = decodePolyline(encodedPolyline);
                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.addAll(points);
                    polylineOptions.width(8);
                    polylineOptions.color(Color.BLUE);
                    Polyline polyline = googleMap.addPolyline(polylineOptions);

                    // Mostrar el tiempo estimado en el TextView
                    int minutes = durationInSeconds / 60;
                    int seconds = durationInSeconds % 60;
                    String formattedTime = String.format("%02d:%02d", minutes, seconds);
                    contadorTiempo.setText("Tiempo estimado: " + formattedTime);
                    contadorTiempo.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(MapActivity.this, "No se encontró la ruta", Toast.LENGTH_SHORT).show();
                }
                // Iniciar la cuenta regresiva
                startCountdownTimer(durationInSeconds);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Decodifica la polyline codificada en la respuesta JSON
        private List<LatLng> decodePolyline(String encodedPolyline) {
            List<LatLng> points = new ArrayList<>();
            int index = 0;
            int lat = 0;
            int lng = 0;

            while (index < encodedPolyline.length()) {
                int b;
                int shift = 0;
                int result = 0;
                do {
                    b = encodedPolyline.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encodedPolyline.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                double latitude = lat / 1E5;
                double longitude = lng / 1E5;
                LatLng point = new LatLng(latitude, longitude);
                points.add(point);
            }
            return points;
        }
    }

    // Métodos del ciclo de vida
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Establecemos una "escucha de clics" en el objeto Google Map.
    private void setMapClickListener() {
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Obtener las coordenadas de latitud y longitud del lugar donde se hizo clic
                double lat = latLng.latitude;
                double lng = latLng.longitude;

                // Convertir las coordenadas a una dirección utilizando Geocoder
                Geocoder geocoder = new Geocoder(MapActivity.this);
                try {
                    List<Address> addressList = geocoder.getFromLocation(lat, lng, 1);
                    if (!addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        String locationName = address.getAddressLine(0);

                        // Mostramos el mensaje de confirmación al usuario
                        showConfirmationDialog(latLng);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        popupMenu.getMenu().add("Vincular User").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Abrir la actividad ProfileActivity al seleccionar la opción 1
                Intent intent = new Intent(MapActivity.this, ProfileActivity.class);
                Toast.makeText(MapActivity.this, "Mostrando la configuracion de usuarios vinculados", Toast.LENGTH_SHORT).show();
                startActivity(intent);
                return true;
            }
        });

        popupMenu.getMenu().add("Ver notificaciones enviadas").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MapActivity.this, VisualizacionLista.class);
                startActivity(intent);
                return true;
            }
        });

        popupMenu.show();
    }

    // Método para enviar una notificación a los contactos vinculados
    private void enviarNotificacionConUbicacion() {
        // Obtén la instancia de FirebaseAuth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Obtén el usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // El usuario está autenticado, obtenemos su ID
            String userId = currentUser.getUid();

            // Creamos una referencia a la colección "usuarios" en Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference usuariosRef = db.collection("usuarios");

            // Obtén los datos del usuario actual desde Firestore
            usuariosRef.document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        List<String> tokensUsuariosVinculados = (List<String>) document.get("tokensUsuariosVinculados");
                        if (tokensUsuariosVinculados != null && !tokensUsuariosVinculados.isEmpty()) {
                            // Mostrar una notificación o log para indicar que se han obtenido los tokens correctamente
                            Log.d("Notificación", "Tokens de usuarios vinculados obtenidos correctamente. Total de tokens: " + tokensUsuariosVinculados.size());

                            // Obtener la ubicación actual del usuario
                            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                if (location != null) {

                                    // Cambiar el color de la bolita a rojo
                                    bolitaView.setImageResource(R.drawable.bolita_roja);

                                    // Obtener la dirección desde las coordenadas de ubicación
                                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                                    try {
                                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                        if (addresses != null && !addresses.isEmpty()) {
                                            String addressLine = addresses.get(0).getAddressLine(0);

                                            // Creamos el mensaje de notificación utilizando RemoteMessage para cada token
                                            for (String token : tokensUsuariosVinculados) {
                                                RemoteMessage.Builder notificationMessage = new RemoteMessage.Builder(token + "@gcm.googleapis.com");
                                                notificationMessage
                                                        .setMessageId(Integer.toString(new Random().nextInt(9999)))
                                                        .addData("title", "AUXILIO")
                                                        .addData("body", "Ubicación: " + addressLine);

                                            // Construimos el mensaje
                                                RemoteMessage message = notificationMessage.build();

                                                // Enviamos el mensaje a través de FirebaseMessaging
                                                try {
                                                    FirebaseMessaging.getInstance().send(message);
                                                    // Mostrar una notificación o log para indicar que se ha enviado la notificación
                                                    Log.d("Notificación", "Notificación enviada con éxito a: " + token);
                                                    mostrarToast("Notificación enviada con éxito");
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    // Mostrar una notificación o log para indicar que ha ocurrido un error al enviar la notificación
                                                    Log.e("Notificación", "Error al enviar la notificación a: " + token, e);
                                                    mostrarToast("Error al enviar la notificación");
                                                }
                                            }
                                            // Obtén la referencia a la colección "NotificacionesEnviadas" en Firestore
                                            CollectionReference notificacionesEnviadasRef = db.collection("NotificacionesEnviadas");

                                            // Crea un objeto con la información de la notificación enviada
                                            Map<String, Object> notificacionData = new HashMap<>();
                                            notificacionData.put("userId", userId); // ID del usuario al que se envió la notificación
                                            notificacionData.put("contenido", "AUXILIO: " + addressLine); // Contenido del mensaje de la notificación con la ubicación
                                            notificacionData.put("fecha", new Date()); // Fecha y hora de envío (puedes usar new Date() para obtener la fecha actual)
                                            notificacionData.put("usuariosDestinatarios", tokensUsuariosVinculados);

                                            // Agregar el documento a la colección "NotificacionesEnviadas"
                                            notificacionesEnviadasRef.add(notificacionData)
                                                    .addOnSuccessListener(documentReference -> {
                                                        Log.d("Notificación", "Registro de notificación enviada agregado a la base de datos con ID: " + documentReference.getId());
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("Notificación", "Error al agregar el registro de notificación enviada a la base de datos", e);
                                                    });

                                        } else {
                                            Log.d("Notificación", "No se pudo obtener la dirección desde las coordenadas de ubicación.");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.e("Notificación", "Error al obtener la dirección desde las coordenadas de ubicación.", e);
                                    }
                                } else {
                                    Log.d("Notificación", "No se pudo obtener la ubicación actual del usuario.");
                                }
                            } else {
                                Log.d("Notificación", "No se han otorgado los permisos de ubicación necesarios.");
                            }
                        } else {
                            Log.d("Notificación", "El usuario actual no tiene usuarios vinculados o no hay tokens disponibles.");
                        }
                    } else {
                        Log.d("Notificación", "El usuario actual no existe en Firestore.");
                    }
                } else {
                    // Manejar el caso de error en la consulta a Firestore
                    Toast.makeText(this, "Error al obtener los datos del usuario actual", Toast.LENGTH_SHORT).show();
                    Log.e("Notificación", "Error al obtener los datos del usuario actual", task.getException());
                }
            });
        }
    }

    private void mostrarToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
