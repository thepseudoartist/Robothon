package blueshark.app.robothon.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Scanner;

import blueshark.app.robothon.R;

public class MapActivity extends BaseActivity implements OnMapReadyCallback, LocationListener {

    MapView mapView;
    MapboxMap mapboxMap;
    boolean firstLoc = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapView = findViewById(R.id.map_view);
        mapView.getMapAsync(this);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_map;
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.SATELLITE_STREETS, style -> new LoadGeoJson(MapActivity.this, 0).execute());

        Gps();

        ArrayList<String> points = new ArrayList<>();

        points.add("81.60747528076172,21.24474238593045");
        points.add("81.60344123840332,21.250162132562014");
        points.add("81.60747528076172,21.24474238593045");
        points.add("81.63399696350098,21.23802242331219");
        points.add("81.60284042358398,21.248302241977623");

        for(String s : points){
            mapboxMap.addMarker(new MarkerOptions().setPosition(new LatLng(
                    Double.valueOf(s.split(",")[0]),
                    Double.valueOf(s.split(",")[1]))));
        }
    }

    private void drawLines(@NonNull FeatureCollection featureCollection) {
        if (mapboxMap != null) {

            mapboxMap.getStyle(style -> {
                if (featureCollection.features() != null) {
                    if (featureCollection.features().size() > 0) {
                        style.addSource(new GeoJsonSource("line-source", featureCollection));

                        style.addLayer(new LineLayer("linelayer", "line-source")
                                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                        PropertyFactory.lineOpacity(.7f),
                                        PropertyFactory.lineWidth(7f),
                                        PropertyFactory.lineColor(Color.parseColor("#3bb2d0"))));
                    }
                }
            });
        }
    }

    private static class LoadGeoJson extends AsyncTask<Void, Void, FeatureCollection> {

        private WeakReference<MapActivity> weakReference;
        private int i;

        LoadGeoJson(MapActivity activity, int i) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        protected FeatureCollection doInBackground(Void... voids) {
            try {
                MapActivity activity = weakReference.get();
                if (activity != null) {
                    InputStream inputStream = activity.getAssets().open("user1.geojson");
                    return FeatureCollection.fromJson(convertStreamToString(inputStream));
                }
            } catch (Exception exception) {
                Log.e("Exception GeoJSON: %s", exception.toString());
            }

            return null;
        }

        static String convertStreamToString(InputStream is) {
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

        @Override
        protected void onPostExecute(@Nullable FeatureCollection featureCollection) {
            super.onPostExecute(featureCollection);
             MapActivity activity = weakReference.get();

            if (activity != null && featureCollection != null) {
                activity.drawLines(featureCollection);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition position = new CameraPosition.Builder()
                .target(latLng)
                .zoom(13)
                .bearing(180)
                .tilt(30)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000);

        if(firstLoc){
            mapboxMap.addMarker(new MarkerOptions().setPosition(new LatLng(location.getLatitude(), location.getLongitude())));
            firstLoc = false;
        }
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

    private void Gps() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        }

        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);

        Location location = null;

        if (bestProvider != null) {
            location = locationManager.getLastKnownLocation(bestProvider);
        }

        if (location == null) {
            Toast.makeText(getApplicationContext(), "GPS signal not found", Toast.LENGTH_LONG).show();
        }

        if (location != null) {
            onLocationChanged(location);
        }
    }
}
