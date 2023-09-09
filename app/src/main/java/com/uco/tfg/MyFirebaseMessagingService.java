package com.uco.tfg;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    private static final String COMMON_CHANNEL_ID = "channel_id"; // ID del canal de notificación

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("MyNotificationService", "onMessageReceived called");

        if (remoteMessage.getNotification() != null) {
            Log.d("MyNotificationService", "Received notification: " + remoteMessage.getNotification().getTitle() + " - " + remoteMessage.getNotification().getBody());

            mostrarNotificacion(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }


    private void mostrarNotificacion(String title, String body) {
        Log.d("MyFirebaseMessaging", "Mostrando notificación: " + title + " - " + body);

        // Obtener el servicio de notificación
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        // Intent para abrir la actividad principal de tu aplicación cuando se toca la notificación
        Intent intent = new Intent(this, MainActivity.class); // Reemplaza "MainActivity" con la clase de tu actividad principal
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);


        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, COMMON_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon) // Icono de la notificación (debes proporcionar el recurso)
                .setContentTitle(title) // Título de la notificación
                .setContentText(body) // Cuerpo de la notificación
                .setAutoCancel(true) // Permite que la notificación sea cancelable al tocarla
                .setPriority(NotificationCompat.PRIORITY_DEFAULT); // Prioridad de la notificación

        Log.d("MyFirebaseMessaging", "Mostrando la notificación...");

        // Mostrar la notificación
        notificationManager.notify(0, builder.build()); // Puedes usar un ID diferente para cada notificación si lo deseas
    }
}


