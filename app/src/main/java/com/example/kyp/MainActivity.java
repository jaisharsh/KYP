package com.example.kyp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicMarkableReference;

import static android.location.LocationManager.*;

public class MainActivity extends AppCompatActivity {
    TextView IMEI;
    TextView IC;
    ImageView myimage;
    Button btn;
    NetworkInfo ni;
    TextView TS;
    LocationManager locationManager;
    String id;
    String city;
    String country;
    TelephonyManager tm;
    ConnectivityManager cm;
    private TextView battery;
    private TextView batterystatus;
    FusedLocationProviderClient fusedLocationProviderClient;
    TextView add;
    public  static final int REQUEST_CODR = 100;
    private StorageReference storageRef;

    private BroadcastReceiver batterylevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            battery.setText(String.valueOf(level) + "%");
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (status == 2) {
                batterystatus.setText(String.valueOf("Charging"));
            } else {
                batterystatus.setText(String.valueOf("Not Charging"));
            }
            cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnectedOrConnecting()) {
                if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    IC.setText("Connected via WiFi");
                } else if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                    IC.setText("Connected via Mobile Data");
                }
            } else {
                IC.setText("Not Connected");
            }

            SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss, dd-MM-yyyy");
            String format = s.format(new Date());
            TS.setText(format);
        }
    };

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnectedOrConnecting()) {
                if (ni.getType() == ConnectivityManager.TYPE_WIFI) {
                    IC.setText("Connected via WiFi");
                } else if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
                    IC.setText("Connected via Mobile Data");
                }
            } else {
                IC.setText("Not Connected");
            }

            SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss, dd-MM-yyyy");
            String format = s.format(new Date());
            TS.setText(format);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IMEI = (TextView) findViewById(R.id.imei);
        myimage = (ImageView) findViewById(R.id.imageView);
        IC = (TextView) findViewById(R.id.ic);
        TS = (TextView) findViewById(R.id.ts);
        add = (TextView) findViewById(R.id.loc);
        btn =(Button) findViewById(R.id.button);
        int permis = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if(permis == PackageManager.PERMISSION_GRANTED) {
            tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//            id = tm.getDeviceId().toString();
            id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//            id = tm.getImei().toString();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 123);
        }
        IMEI.setText(id.toString());

        battery = (TextView)findViewById(R.id.level);
        batterystatus = (TextView)findViewById(R.id.status);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addressList = null;
                    try {
                        addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        city = addressList.get(0).getLocality();
                        country = addressList.get(0).getCountryName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    add.setText(city + ", " + country);
                }
            });

        }

        else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_CODR);
        }
        this.registerReceiver(this.batterylevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        this.registerReceiver(this.timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        storageRef = FirebaseStorage.getInstance().getReference();

    }


    public void uploadImage(View view) {
        Intent icamera = new Intent();
        icamera.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(icamera, 201);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if(requestCode == 201){
                onCaptureImageResult(data);
            }
        }
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        byte bb[] = bytes.toByteArray();
        myimage.setImageBitmap(thumbnail);

        uploadToFirebase(bb);
    }

    private void uploadToFirebase(byte[] bb) {
        StorageReference sr = storageRef.child("myimages/harsh.jpg");
        sr.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "Successfully Uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to Upload", Toast.LENGTH_SHORT).show();
            }
        });
    }
}