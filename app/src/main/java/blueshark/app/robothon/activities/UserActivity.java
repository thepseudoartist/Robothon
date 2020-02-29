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
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.daimajia.numberprogressbar.NumberProgressBar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
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
import java.sql.Time;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import blueshark.app.robothon.R;
import blueshark.app.robothon.models.UserIDetails;
import blueshark.app.robothon.models.UserStatusResponse;
import blueshark.app.robothon.restapi.APIServices;
import blueshark.app.robothon.restapi.AppClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserActivity extends BaseActivity implements OnMapReadyCallback, LocationListener {
    AnyChartView tdsChart;
    Set set;
    TextView pH, temp, tds;
    NumberProgressBar progressBar;

    MapView mapView;
    MapboxMap mapboxMap;

    List<DataEntry> seriesData = new ArrayList<>();

    boolean first = true;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
    }

    private void init() {
        progressBar = findViewById(R.id.num_prog);
        progressBar.setProgressTextVisibility(NumberProgressBar.ProgressTextVisibility.Invisible);
        progressBar.setReachedBarColor(Color.parseColor("#2e3663"));

        pH = findViewById(R.id.ph_text);
        temp = findViewById(R.id.temp_text);
        tds = findViewById(R.id.tds_text);

        getChart();

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                apiCall();
                handler.postDelayed(this, 5000);
            }
        };

        handler.post(runnable);

        mapView = findViewById(R.id.map_view);
        mapView.getMapAsync(this);
    }

    private void getChart() {
        tdsChart = findViewById(R.id.tds_chart);

        Cartesian cartesian = AnyChart.line();

        cartesian.animation(true);

        cartesian.padding(10d, 20d, 5d, 20d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);

        cartesian.title("Live Data");

        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        seriesData.add(new CustomDataEntry(String.valueOf(System.currentTimeMillis()), 25, 200, 7.0));

        set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Mapping = set.mapAs("{ x: 'x', value: 'value2' }");
        Mapping series3Mapping = set.mapAs("{ x: 'x', value: 'value3' }");

        Line series1 = cartesian.line(series1Mapping);
        series1.name("Temperature");
        series1.hovered().markers().enabled(true);
        series1.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);

        series1.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series2 = cartesian.line(series2Mapping);
        series2.name("TDS");
        series2.hovered().markers().enabled(true);
        series2.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);

        series2.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line series3 = cartesian.line(series3Mapping);
        series3.name("pH");
        series3.hovered().markers().enabled(true);
        series3.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(4d);

        series3.tooltip()
                .position("right")
                .anchor(Anchor.LEFT_CENTER)
                .offsetX(5d)
                .offsetY(5d);

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(13d);
        cartesian.legend().padding(0d, 0d, 10d, 0d);
        cartesian.background().fill(new String[]{"#07091E", "#07091E", "#1a2254"}, 90, true, 100);
        cartesian.autoRedraw(true);
        cartesian.xAxis(0).labels().enabled(false);

        tdsChart.setZoomEnabled(true);
        tdsChart.setChart(cartesian);
    }

    private void addData(UserStatusResponse response) {
        set.append("{x: " + System.currentTimeMillis() + ", value: " + response.temp + ", value2: " + response.tds + ", value3: " + response.pH + "}");
        seriesData.add(new DataEntry());

        if (seriesData.size() >= 10) {
            for(int i = 0; i < 6; i++) set.remove(i);
        }

        if (first) {
            set.remove(0);
            first = false;
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.DARK, style -> new LoadGeoJson(UserActivity.this, 0).execute());

        Gps();
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

        private WeakReference<UserActivity> weakReference;
        private int i;

        LoadGeoJson(UserActivity activity, int i) {
            this.weakReference = new WeakReference<>(activity);
        }

        @Override
        protected FeatureCollection doInBackground(Void... voids) {
            try {
                UserActivity activity = weakReference.get();
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
            UserActivity activity = weakReference.get();

            if (activity != null && featureCollection != null) {
                activity.drawLines(featureCollection);
            }
        }
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2, Number value3) {
            super(x, value);

            setValue("value2", value2);
            setValue("value3", value3);
        }

    }

    private void apiCall() {

        UserIDetails details = new UserIDetails();
        details.uId = 1;

        APIServices apiServices = AppClient.getInstance().createService(APIServices.class);
        Call<UserStatusResponse> responseCall = apiServices.getUserStatus(details);

        responseCall.enqueue(new Callback<UserStatusResponse>() {
            @Override
            public void onResponse(Call<UserStatusResponse> call, Response<UserStatusResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    addData(response.body());

                    pH.setText((int) response.body().pH + " ");
                    temp.setText((int) response.body().temp + "°C");
                    tds.setText((int) response.body().tds + " NTU");
                    progressBar.setProgress((int) response.body().pH);
                }
            }

            @Override
            public void onFailure(Call<UserStatusResponse> call, Throwable t) {

            }
        });
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
                .zoom(15)
                .bearing(180)
                .tilt(30)
                .build();

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 7000);
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

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);

        Location location = locationManager.getLastKnownLocation(bestProvider);

        if (location == null) {
            Toast.makeText(getApplicationContext(), "GPS signal not found", Toast.LENGTH_LONG).show();
        }

        if (location != null) {
            onLocationChanged(location);
        }
    }
}
