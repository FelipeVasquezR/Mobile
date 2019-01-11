package redescomputacionales.cl.appredeswifi;

import android.annotation.SuppressLint;
import android.provider.Settings.Secure;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.location.LocationManager;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMarkerDragListener, AdapterView.OnItemSelectedListener {

    //Se agrega un MapFragment y un GoogleMap
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;

    //Obtiene la posición actual
    private FusedLocationProviderClient mFusedLocationClient;

    //Verificar servicos GPS
    private LocationManager locationManager;

    //Posición GPS del dispositivo
    private LatLng gpsLocation;

    private String[] salas = new String[]{"Sala 201", "Sala 517", "Sala 519", "Sala 520", "Sala 564", "Libre"};

    private String sala = "nada";

    //Clase WifiInfo con los metodos para obtener datos de la coneccion
    private WifiManager wifiManager;
    private WifiInfo connectionInfo;

    String fechaHora;

    //Datos wifi solicitados
    private double speed;
    private double intensidad;

    //Estado de la red
    String estado;

    String info;

    private static final String TAG = "ConnectionClass-Sample";

    private ConnectionClassManager mConnectionClassManager;
    private DeviceBandwidthSampler mDeviceBandwidthSampler;
    private ConnectionChangedListener mListener;
    private View mRunningBar;

    //private String mURL = "https://apod.nasa.gov/apod/fap/image/1505/LakeMyvatn_Brady_3840.jpg";
    private String mURL = "https://www.usach.cl/sites/default/files/logo_usach.jpg";
    private int mTries = 0;
    private ConnectionQuality mConnectionClass = ConnectionQuality.UNKNOWN;

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DownloadImage().execute(mURL);
                //saveData(view);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Soporte para el MapFragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map); //R.id.map es un fragmento que está en content_main.xml
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        //Para obtener la última (o actual) ubicacion conocida
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getSupportActionBar().setTitle("Registrar estado de red");

        mConnectionClassManager = ConnectionClassManager.getInstance();
        mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();
        mRunningBar = findViewById(R.id.runningBar);
        mRunningBar.setVisibility(View.GONE);
        mListener = new ConnectionChangedListener();

        Spinner sp = findViewById(R.id.spinner);
        sp.setOnItemSelectedListener(this);

        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, this.salas);
        //set the spinners adapter to the previously created one.
        sp.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Build myBuild = new Build();

        String myModel = myBuild.MODEL;

        String id = Settings.Secure.ANDROID_ID;

        Build.VERSION version = new Build.VERSION();

        String myVersion = version.RELEASE;

        String myBrand = myBuild.BRAND;

        String myProduct = myBuild.PRODUCT;
        Toast.makeText(this, myBrand+" "+myModel+" "+myVersion+" "+this.sala, Toast.LENGTH_LONG).show();
        setUserData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConnectionClassManager.remove(mListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionClassManager.register(mListener);
    }

    public void setUserData()
    {
        //oogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView mainHeader = (TextView) headerView.findViewById(R.id.mainHeaderId);
        TextView subHeader = (TextView) headerView.findViewById(R.id.subHeaderId);
        ImageView imageView = (ImageView) headerView.findViewById(R.id.imageView);

        Build myBuild = new Build();

        String myModel = myBuild.MODEL;

        System.out.println(myModel);

        String myDevice = myBuild.DEVICE;
        System.out.println(myDevice);

        String myBrand = myBuild.BRAND;

        System.out.println(myBrand);

        String myProduct = myBuild.PRODUCT;

        System.out.println(myProduct);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog);
        dialog.setCancelable(false);
        dialog.setTitle("Prueba de las variables");
        dialog.setMessage(myDevice+" "+myBrand+" "+myModel+" "+myProduct);


        /*if(account != null)
        {
            mainHeader.setText(account.getDisplayName());
            subHeader.setText(account.getEmail());
            Picasso.get().load(account.getPhotoUrl()).into(imageView);
        }
        else
        {*/
            mainHeader.setText("AppRedesWiFi");
            subHeader.setText("Redes Computacionales 1-2018");
            imageView.setImageResource(R.mipmap.ic_launcher);
        //}
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        item.setCheckable(false);
        if (id == R.id.registros) {
            Intent registros = new Intent(MainActivity.this, RegistrosActivity.class);
            if (registros != null) {
                startActivity(registros);
            } else {
                Toast.makeText(this, "Ha ocurrido un error", Toast.LENGTH_SHORT).show();
            }
        }

        else if(id == R.id.logout)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog);
            dialog.setCancelable(false);
            dialog.setTitle("Salir de AppRedesWiFi");
            dialog.setMessage("¿Está seguro que desea cerrar sesión?");
            dialog.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    //Action for positive.
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("ACTION", "signout");
                    startActivity(intent);
                    finish();
                }
            }).setNegativeButton("No ", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Action for negative.
                }
            });
            final AlertDialog alert = dialog.create();
            alert.show();
        }

        else if (id == R.id.acercaDe) {
            Toast.makeText(this, "Universidad de Santiago de Chile\nDepartamento de Ingeniería Informática\nRedes Computacionales 1-2018", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMarkerDragListener(this);
        getCurrentLocation();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        mMap.clear();
        getCurrentLocation();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        gpsLocation = marker.getPosition();
    }


    public void updateGpsLocation(Location location) {
        //Asigna la LatLng a partir la ubicación GPS
        gpsLocation = new LatLng(location.getLatitude(), location.getLongitude());
    }

    public void addMarkerToLocation(Location location, String tittle) {
        //Añade un marcador en el mapa
        mMap.addMarker(new MarkerOptions()
                .position(gpsLocation)
                .title(tittle)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                .draggable(true)
        );
    }

    public void moveCameraToLocation(Location location) {
        //Mueve el mapa con movimiento suave
        CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(
                gpsLocation, 18);
        mMap.animateCamera(camara);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation()
    //Obtiene la posición actual y añade un marcador en el mapa
    {
        //Obtiene la posicion actual
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            updateGpsLocation(location);
                            addMarkerToLocation(location, "Posición Actual");
                            moveCameraToLocation(location);
                        }
                    }
                });
    }


    public void saveData()
    {
        //Verifica si el wifi esta activado
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) //Si está apagado
        {
            Toast.makeText(MainActivity.this, "Conexión Wi-Fi apagada\nPor favor, conéctese a una red WiFi", Toast.LENGTH_SHORT).show();
            return;
        } else { //Si está predindo
            connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo.getNetworkId() == -1) { //Si está prendido pero no conectado
                Toast.makeText(MainActivity.this, "No hay conexión Wi-Fi\nPor favor, conéctese a una red WiFi para continuar", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Log.i("> Wifi", "Conectado a Wi-Fi");

        //Verifica si la red a la que esta conectada es de la usach
        String wifiName = wifiManager.getConnectionInfo().getSSID();
        Log.i("> WIFI NAME", wifiName);
        /*if(!wifiName.equals("\"USACH-Alumnos\""))
        {
            Toast.makeText(MainActivity.this, "La red Wi-Fi no es USACH-Alumnos\nPor favor, conéctese a esta red", Toast.LENGTH_SHORT).show();
            return;
        }*/


        //Verifica si el GPS esta activado
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) //Si está apagado
        {
            Toast.makeText(MainActivity.this, "Ubicación GPS apagada\nPor favor, encienda la ubicación GPS para continuar", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i("> GPS", "Ubicacion GPS activada");


        //Si la ubicacion gps es nula, no hace nada
        if(gpsLocation == null)
        {
            Toast.makeText(MainActivity.this, "Por favor, indique su posición en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i("> GPS", "Ubicacion GPS no es nula");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        fechaHora = sdf.format(new Date());

        intensidad = (double) connectionInfo.getRssi();

        final Context context = getApplicationContext();
        final CharSequence postCorrect = "Datos ingresados correctamente";
        final CharSequence postError= "Ha ocurrido un error con la subida de datos";
        final int shortDuration = Toast.LENGTH_SHORT;
        final int longDuration = Toast.LENGTH_LONG;

        // Instantiate the RequestQueue.
        final RequestQueue[] queue = {Volley.newRequestQueue(MainActivity.this)};
        //this is the url where you want to send the request
        String url = "http://kamino.diinf.usach.cl/redes-0.0.1-SNAPSHOT/signals";

        //Device characterists
        Build myBuild = new Build();

        String myModel = myBuild.MODEL;

        Build.VERSION version = new Build.VERSION();

        String myVersion = "Android " + version.RELEASE;

        String myBrand = myBuild.BRAND;

        String _latitud = String.valueOf(gpsLocation.latitude);
        String _longitud = String.valueOf(gpsLocation.longitude);
        String _fecha = fechaHora;
        //Toast toastPlain = Toast.makeText(context, _fecha, shortDuration);
        //toastPlain.show();
        String estado = this.estado;
        String _velocidad = String.valueOf(speed);
        String _intensidad = String.valueOf(intensidad);
        Calendar cal = Calendar.getInstance();
        cal.setTime(Calendar.getInstance().getTime());
        int horas = cal.getTime().getHours();
        String bloque;
        if(horas >= 8 && horas < 12)
        {
            bloque = "Mañana";
        }
        else if(horas >= 12 && horas < 18)
        {
            bloque = "Tarde";
        }
        else
        {
            bloque = "Noche";
        }
        String dia;
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int lunes = Calendar.MONDAY;
        int martes = Calendar.TUESDAY;
        int mier = Calendar.WEDNESDAY;
        int jueves = Calendar.THURSDAY;
        if(day == lunes)
        {
            dia = "Lunes";
        }
        else if(day == martes)
        {
            dia = "Martes";
        }
        else if(day == mier)
        {
            dia = "Miercoles";
        }
        else if(day == jueves)
        {
            dia = "Jueves";
        }
        else
        {
            dia = "Viernes";
        }
        String android_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        JSONObject postparams=new JSONObject();

        try {
            postparams.put("longitud", _longitud);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("latitud", _latitud);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("fecha", _fecha);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("estado", estado);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("lugar", this.sala);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("marca", myBrand);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("modelo", myModel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("version", myVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("velocidad", _velocidad);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("intensidad", _intensidad);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("idDevice", android_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("dia", dia);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            postparams.put("bloque", bloque);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, postparams, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        mRunningBar.setVisibility(View.GONE);
                        Toast toastPlain = Toast.makeText(context, postCorrect, shortDuration);
                        toastPlain.show();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mRunningBar.setVisibility(View.GONE);
                        Toast toastPlain = Toast.makeText(context, postError, shortDuration);
                        toastPlain.show();
                    }
                });
        RequestQueue r = Volley.newRequestQueue(getApplicationContext());

        jsonObjectRequest.setTag("postRequest");

        r.add(jsonObjectRequest);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        this.sala = salas[i];
        Toast.makeText(this, this.sala, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    /**
     * Listener to update the UI upon connectionclass change.
     */
    private class ConnectionChangedListener
            implements ConnectionClassManager.ConnectionClassStateChangeListener {

        @Override
        public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
            mConnectionClass = bandwidthState;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    estado = mConnectionClass.toString();
                }
            });
        }
    }

    /**
     * AsyncTask for handling downloading and making calls to the timer.
     */
    private class DownloadImage extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            fab.hide();
            mDeviceBandwidthSampler.startSampling();
            mRunningBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... urlArray) {
            String imageURL = urlArray[0];
            try {
                // Open a stream to download the image from our URL.
                URLConnection connection = new URL(imageURL).openConnection();
                connection.setUseCaches(false);
                connection.connect();
                InputStream input = connection.getInputStream();
                try {
                    byte[] buffer = new byte[1024];
                    // Do some busy waiting while the stream is open.
                    while (input.read(buffer) != -1) {
                    }
                } finally {
                    input.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error while downloading image.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            fab.show();
            mDeviceBandwidthSampler.stopSampling();
            // Retry for up to 10 times until we find a ConnectionClass.
            if (mConnectionClass == ConnectionQuality.UNKNOWN && mTries < 10) {
                mTries++;
                new DownloadImage().execute(mURL);
            }
            if (!mDeviceBandwidthSampler.isSampling()) {
                mRunningBar.setVisibility(View.GONE);
                speed = mConnectionClassManager.getDownloadKBitsPerSecond();
                saveData();
                //mConnectionClass = ConnectionQuality.UNKNOWN;
                //mTries = 0;
            }

        }
    }
}


