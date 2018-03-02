package com.example.uceda.geomap;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;

import java.io.IOException;

//servicio de localizacion es el que escucha y guarda las localizaciones. Los servicios normales, yo
// le puedo decir que me obtenga una posicion cada 10 seg y no me hace ni puto caso, lo hace cuando
// quiera. Con un servicio foreground, no tienes ese problema.
public class LocationService extends Service {

    private final static int LOCATION_SERVICE_ID = 1;

    private static final String TAG = LocationService.class.getSimpleName();

    private ObjectContainer objectContainer;

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() { //se crea una vez cuando lanzo el servicio
        super.onCreate();
        Intent i=new Intent(this, LocationService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Notification.Builder constructorNotificacion = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Location")//ponle otro titulo
                .setContentText("Registrando location en Db4o") //
                .setContentIntent(PendingIntent.getActivity(this, 0, i, 0));
        startForeground(LOCATION_SERVICE_ID, constructorNotificacion.build()); //le ha puesto que no haga nada, ponle que haga algo, programalo.
        // Convierto mi servicio en un foreground
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //do the job de mi geolocalizacion, ResopleApiException.
        //iniciar la locali, obtener las localiz, y guardarlas en la BBDD.
        Bundle extras = intent.getExtras();
        if(extras == null) {
            Log.d(TAG,"Intent null");
            return START_STICKY;
        }

        Location location = (Location) extras.get("location");
        Localizacion localizacion = new Localizacion(location);

        objectContainer = openDataBase(getApplicationContext());
        Db4oUtils.storeInDb4oDataBase(objectContainer, localizacion);

        Db4oUtils.closeDb4oDataBase(objectContainer);

        this.stopSelf();
        return START_STICKY; //si lo ha matado el servicio, START_STICKY en cuanto puedas reviveme y sigue cogiendo posiciones y guardandolas
    }

    public ObjectContainer openDataBase(Context context) { //abre la conexion con tu BBDD
        ObjectContainer objectContainer = null;
        try {
            String name = Db4oUtils.db4oDBFullPath(context);
            objectContainer = Db4oEmbedded.openFile(Db4oUtils.getDb4oConfig(), name);
        } catch (IOException e) {
            Log.v(TAG, e.toString());
        }
        return objectContainer;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service destroyed.");
        super.onDestroy();
    }

}
