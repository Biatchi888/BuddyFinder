package com.example.ankitjha.buddyfinder;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;

public class LocServiceClass extends Service {

    LocationManager locManager;
    LocationListener netLis, gpsLis;
    Location userLoc;
    static CountDownTimer c;
    int userLoggedIn;

    private void listenerAssigning() {
        netLis = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (userLoc == null)
                    userLoc = location;
                else if (userLoc.getAccuracy() <= location.getAccuracy())
                    userLoc = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        gpsLis = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (userLoc == null)
                    userLoc = location;
                else if (userLoc.getAccuracy() <= location.getAccuracy())
                    userLoc = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final int uid = intent.getIntExtra("userID", -1);
        userLoggedIn = uid;

        if (uid == -1) {
            Toast.makeText(getApplicationContext(), "Error!!", Toast.LENGTH_SHORT).show();
            stopSelf();
        }

        ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean stat = connectivitymanager.getActiveNetworkInfo().isConnected();
        if (stat == false) {
            return super.onStartCommand(intent, flags, startId);
        }

        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        listenerAssigning();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location net, gps;
            gps = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            net = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps == null)
                userLoc = net;
            else if (net == null)
                userLoc = gps;
            else if (gps.getAccuracy() > net.getAccuracy())
                userLoc = gps;
            else
                userLoc = net;

            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsLis);
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, netLis);
        } else {
            Toast.makeText(getApplicationContext(), "Give Permissions for location", Toast.LENGTH_SHORT).show();
            stopSelf();
        }

        c = new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                boolean stat = connectivitymanager.getActiveNetworkInfo().isConnected();
                if (stat == false) {
                    start();
                    return;
                }

                DatabaseReference locUpref = FirebaseDatabase.getInstance().getReference("Users");
                Double lat, lon;
                lat = userLoc.getLatitude();
                DecimalFormat df = new DecimalFormat("#.#######");
                lat = Double.valueOf(df.format(lat));
                lon = Double.valueOf(df.format(userLoc.getLongitude()));

                locUpref.child(String.valueOf(uid)).child("lat").setValue(lat);
                locUpref.child(String.valueOf(uid)).child("lng").setValue(lon);

                start();
            }
        };
        c.start();
        startForegroundService();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundService() {
        Intent locIntent = new Intent(getApplicationContext(), LocationPreferenceActivity.class);
        locIntent.putExtra("userID", userLoggedIn);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT; // or any other flags you require
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags |= PendingIntent.FLAG_MUTABLE; // Add FLAG_MUTABLE for Android S and above
        }

        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, locIntent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "loc_setting")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Location Sharing")
                .setContentText("Your Location is being Shared.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Your Location is being shared. Go to Location Sharing Preference to Disable Sharing Your Location"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(pendIntent);

        NotificationManagerCompat compat = NotificationManagerCompat.from(this);
        compat.notify(0, builder.build());
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        c.cancel();
        NotificationManagerCompat compat = NotificationManagerCompat.from(this);
        compat.cancel(0);
        locManager.removeUpdates(netLis);
        locManager.removeUpdates(gpsLis);
        super.onTaskRemoved(rootIntent);
    }
}
