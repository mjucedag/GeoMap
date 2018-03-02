package com.example.uceda.geomap;
//obtener la geolocalizacion del dispositivo movil, la guardas en una BBDD db4o, introduce el dia y pinta el poliline de
//la ruta

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.AndroidSupport;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.query.Predicate;
import com.db4o.query.Query;

import java.io.IOException;
import java.util.GregorianCalendar;

public class Db4oActivity extends AppCompatActivity {

    private static final String TAG = Db4oActivity.class.getSimpleName();

    private ObjectContainer objectContainer;

    public EmbeddedConfiguration getDb4oConfig() throws IOException {
        EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
        configuration.common().add(new AndroidSupport());
        configuration.common().objectClass(Localizacion.class).
                objectField("fecha").indexed(true);
        return configuration;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db4o);
        //necesitas la libreria pulsa android y pulsa project,

        startService(new Intent(this, LocationService.class));

        objectContainer = openDataBase("geomap.db4o");

        Localizacion loc = new Localizacion();
        objectContainer.store(loc);
        objectContainer.commit();

        loc = new Localizacion(new Location("provider"));
        objectContainer.store(loc);
        objectContainer.commit();

        loc = new Localizacion(new Location("proveedor"), new GregorianCalendar(2018,1,22).getTime());
        objectContainer.store(loc);
        objectContainer.commit();

        Query consulta = objectContainer.query();
        consulta.constrain(Localizacion.class);
        ObjectSet<Localizacion> localizaciones = consulta.execute();
        for(Localizacion localizacion: localizaciones){
            Log.v(TAG, "1: " + localizacion.toString());
        }

        ObjectSet<Localizacion> locs = objectContainer.query(
                new Predicate<Localizacion>() { //predicate tiene que ser true para que me devuelva el objeto de la bbdd
                    @Override
                    public boolean match(Localizacion loc) {
                        return loc.getFecha().equals(new GregorianCalendar(2018,1,22).getTime());
                    }
                });
        for(Localizacion localizacion: locs){ //saca todas las localiz
            Log.v(TAG, "2: " + localizacion.toString());
        }
        objectContainer.close();//siempre que termines, cierra la conexion porque si no no se guarda bien
    }

    private ObjectContainer openDataBase(String archivo) { //abre la conexion con tu BBDD
        ObjectContainer objectContainer = null;
        try {
            String name = getExternalFilesDir(null) + "/" + archivo;
            objectContainer = Db4oEmbedded.openFile(getDb4oConfig(), name);
        } catch (IOException e) {
            Log.v(TAG, e.toString());
        }
        return objectContainer;
    }
}
