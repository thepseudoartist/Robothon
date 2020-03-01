package blueshark.app.robothon.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.view.View;
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

public class UserActivity extends BaseActivity {
    AnyChartView tdsChart;
    Set set;
    TextView pH, temp, tds, turb, water_type;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    TextView alert;

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
        pH = findViewById(R.id.ph_text);
        temp = findViewById(R.id.temp_text);
        tds = findViewById(R.id.tds_text);
        turb = findViewById(R.id.turb_text);
        water_type = findViewById(R.id.water_top);

        findViewById(R.id.get_loc).setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, MapActivity.class);
            startActivity(intent);
        });

        water_type.setOnClickListener(v -> dialog.show());

        getChart();

        builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.alert_dialog, null);
        alert = view.findViewById(R.id.alert);
        builder.setView(view);
        dialog = builder.create();
        dialog.setCancelable(true);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                apiCall();
                handler.postDelayed(this, 5000);
            }
        };

        handler.post(runnable);
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

        seriesData.add(new CustomDataEntry(String.valueOf(System.currentTimeMillis()), 25, 200));

        set = Set.instantiate();
        set.data(seriesData);
        Mapping series1Mapping = set.mapAs("{ x: 'x', value: 'value' }");
        Mapping series2Mapping = set.mapAs("{ x: 'x', value: 'value2' }");

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
        set.append("{x: " + System.currentTimeMillis() + ", value: " + response.temp + ", value2: " + response.tds + "}");

        if (first) {
            set.remove(0);
            first = false;
        }
    }

    private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String x, Number value, Number value2) {
            super(x, value);

            setValue("value2", value2);
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

                    pH.setText(response.body().pH + " ");
                    temp.setText(response.body().temp + "°C");
                    tds.setText(response.body().tds + " mg/L");
                    turb.setText(response.body().turbidity + " NTU");

                    switch (response.body().level) {
                        case 0:
                            water_type.setText("Drinkable");
                            water_type.setTextColor(Color.parseColor("#8b9b62"));
                            alert.setText("You can drink this water without any treatment.");
                            break;

                        case 1:
                            water_type.setText("Slightly Contaminated");
                            water_type.setTextColor(Color.parseColor("#FFFFEB3B"));
                            alert.setText("Boil the water before drinking");
                            break;

                        case 2:
                            water_type.setText("Highly Contaminated");
                            water_type.setTextColor(getColor(android.R.color.holo_red_dark));
                            alert.setText("Water isn't suitable for drinking and agricultural purposes.");
                            break;
                    }
                }
            }

            @Override
            public void onFailure(Call<UserStatusResponse> call, Throwable t) {

            }
        });
    }
}
