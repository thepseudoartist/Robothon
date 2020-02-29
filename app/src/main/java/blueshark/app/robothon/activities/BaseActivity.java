package blueshark.app.robothon.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mapbox.mapboxsdk.Mapbox;

public abstract class BaseActivity extends AppCompatActivity {

    // Every Activity should extend this not AppCompatActivity

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                             WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        init();
    }

    private void init(){
        Mapbox.getInstance(this, "pk.eyJ1IjoidGhlcHNldWRvYXJ0aXN0IiwiYSI6ImNrNzcyZHY5dDAzM2czZnIxOWczdGtiYzIifQ.jy4ijsk1BK-XIKGl6fendA");

        getPermission();
    }

    private void getPermission() {
        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessCoarseLocationApproved) {
            boolean backgroundLocationPermissionApproved =
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            if (!backgroundLocationPermissionApproved) {
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        9005);
            }
            
        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, 9005);
        }

    }

    protected abstract int getLayoutResourceId();
}
