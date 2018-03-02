package com.example.uceda.geomap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.GregorianCalendar;

public class GPSActivity extends AppCompatActivity {

    private static final int PERMISO_LOCATION = 1;
    private static final String TAG = "xyzx";
    private static final int RESOLVE_RESULT = 2;//podria poner 1 porque lo voy a usar en diferentes sitios

    //EL ESCUCHADOR NO LA HAGO A TRAVES DE LA APP, un boton geolocalizacion lanza un servicio.
    // El escuchador lo hago a través del servicio.

    private FusedLocationProviderClient clienteLocalizacion;
    private LocationCallback callbackLocalizacion;
    private LocationRequest peticionLocalizacion;
    private LocationSettingsRequest ajustesPeticionLocalizacion;
    private SettingsClient ajustesCliente;

    private EditText etDate;
    private Button btPath;

    public GPSActivity() {
    }

    private boolean checkPermissions() { //si tengo el permiso concedido de hacer una localización fina, si no pido los permisos
        int estadoPermisos = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return estadoPermisos == PackageManager.PERMISSION_GRANTED;
    }

    private void init() {

        btPath = findViewById(R.id.btPath);
        etDate = findViewById(R.id.etDate);

        btPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Fecha valida = 2018/02/28

                try{

                    String[] arrayDate = etDate.getText().toString().split("/");
                    int year = Integer.parseInt(arrayDate[0]);
                    int month = Integer.parseInt(arrayDate[1]);
                    int day = Integer.parseInt(arrayDate[2]);

                    GregorianCalendar gc = new GregorianCalendar(year,month-1,day);

                    Intent intent = new Intent(GPSActivity.this, MapsActivity.class);
                    intent.putExtra("fecha", gc);

                    //TODO necesario ???
                    clienteLocalizacion.removeLocationUpdates(callbackLocalizacion);

                    startActivity(intent);

                }catch(Exception e){
                    Log.v(TAG, "Error parsing date: " + e.getMessage());
                }


            }
        });
        if(checkPermissions()) {
            startLocations();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //método que se ejecuta autom cuando vengo de otra actividad
        switch (requestCode) {
            case RESOLVE_RESULT: //al resolver el problema del gps
                switch (resultCode) {
                    case Activity.RESULT_OK: //pudimos resolverlos
                        Log.v(TAG, "Permiso ajustes localización");
                        startLocations();//puedo volver a lanzar el startLocation = lanzas de nuevo la localizacion
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.v(TAG, "Sin permiso ajustes localización");
                        break;
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && requestCode == PERMISO_LOCATION) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) { //si la respuesta es que sí  al pedir el permiso
                startLocations(); //se empieza con la geolocalizacion
            }
        }
    }

    private void requestPermissions() {
        boolean solicitarPermiso = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (solicitarPermiso) {
            Log.v(TAG, "Explicación racional del permiso"); //TAG constante delante de los Logs para buscar rapidamente los msg
            showSnackbar(R.string.app_name, android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(GPSActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISO_LOCATION);//constante para saber si has lanzado la tarea
                }
            });
        } else {
            Log.v(TAG, "Solicitando permiso");
            ActivityCompat.requestPermissions(GPSActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISO_LOCATION);
        }
    }

    private void showSnackbar(final int idTexto, final int textoAccion,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(idTexto),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(textoAccion), listener).show();
    }

    @SuppressLint("MissingPermission")
    private void startLocations(){
        //de las variables de instancia, necesitamos todas menos la de Google Map
        //tenemos que importar la libreria y la pegas en gradle app
        clienteLocalizacion = LocationServices.getFusedLocationProviderClient(this);
        ajustesCliente = LocationServices.getSettingsClient(this);
        clienteLocalizacion.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            //cuando los permisos están dados, pincha en el error boton derecho y suprimir la peticion de permisos
        //para encontrar la primera coordenada, tarda un tiempo. Obtengo la última geolocalización para situarte.
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Log.v(TAG, "última localización: " + location.toString());
                } else {
                    Log.v(TAG, "no hay última localización");
                }
            }
        });
        //el callback de localizacion. El clienteLocalizacion obtiene las localizaciones y se la da a callback
        callbackLocalizacion = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //recoges las localizaciones de aqui y los guardas en una BBDD
                Location location = locationResult.getLastLocation();//localizacion exacta de donde estamos
                Intent serviceIntent  = new Intent(GPSActivity.this, LocationService.class);
                serviceIntent.putExtra("location", location);
                startService(serviceIntent );
            }
        };
        //30 segundos haz una  en vez de 10 segundos
        peticionLocalizacion = new LocationRequest();
        peticionLocalizacion.setInterval(10000);
        peticionLocalizacion.setFastestInterval(5000);//en el intervalo de estos 5 segundos admitiendo un error, aprovechando ese intervalo pueden hacerse más geololizaciones desde otras pantallas
        peticionLocalizacion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//PRIORITY_HIGH_ACCURACY = me da igual la bateria, es de alta precisión

        //el builder. Ya montados los objetos para la geolocalizacion, el builder para construir y lanzar el objeto de peticiones.
        //dependiendo de las cosas activadas en tu móvil, podrás o no geolocalizar. Con este builder intento construir
        //el montaje y lanzaria un mensaje de activalo si no se podría
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(peticionLocalizacion);
        ajustesPeticionLocalizacion = builder.build();

        ajustesCliente.checkLocationSettings(ajustesPeticionLocalizacion)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {//añade un listener si all va bien
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.v(TAG, "Se cumplen todos los requisitos");
                        clienteLocalizacion.requestLocationUpdates(peticionLocalizacion, callbackLocalizacion, null);//lanzo mi clienteLocalizacion, cuando tenga nuevas loc, te informa a traves del objeto callback
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() { //si la cosa falla, susanable por el usuario o no
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED://susanar a traves de mi intervención y
                                Log.v(TAG, "Falta algún requisito, intento de adquisición");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(GPSActivity.this, RESOLVE_RESULT);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.v(TAG, "No se puede adquirir.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: //si no es susanable
                                Log.v(TAG, "Falta algún requisito, que no se puede adquirir.");
                        }
                    }
                });
        }
    }

