package com.example.ticketbus;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.ticketbus.Adapters.BusAdapter;
import com.example.ticketbus.Adapters.BusPassAdapter;
import com.example.ticketbus.Interface.IFirebaseLoadDone;
import com.example.ticketbus.Model.BusItem;
import com.example.ticketbus.Model.BusPassCreationModel;
import com.example.ticketbus.Model.BusPassModel;
import com.example.ticketbus.Model.IDs;
import com.example.ticketbus.Model.LocationItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator;


public class DashboardFragment extends Fragment implements IFirebaseLoadDone {

    RecyclerView recyclerView;

    BusPassAdapter busPassAdapter;
    RecyclerView.LayoutManager layoutManager;

    ScrollingPagerIndicator recyclerIndicator;

    List<BusPassModel> busPassModels;
    public static ArrayAdapter<String> adapterFrom;
    private Button SearchBus;
    private ImageButton btn_swap;
    public static ArrayAdapter<String> adapterTo;
    private Spinner FromLocation, ToLocation;

    private ProgressDialog sendingData, progressDialog;
    private IFirebaseLoadDone iFirebaseLoadDone;

    DatabaseReference databaseReference;

    private List<BusItem> busList;
    private BusAdapter busAdapter;

    private View view;

    private List<IDs> iDs;

    int pos = 0;

    Timer timer;
    TimerTask timerTask;
    float spinner1Y;

    float spinner2Y;

    private String fromLocation,toLocation;

    LinearLayout fromLocLayout, toLocLayout;

    boolean swapped = false;

    float linearLayout1Y, linearLayout2Y;

    LinearLayout.LayoutParams params1, params2;

    int count = 0;

    ImageView fromLocIcon, toLocIcon;

    boolean paused;

    Vibrator vibrator;

    DialogPlus dialogPlus;

    //--------------------------

    Button mapBtn;

    private Context context;
    private LocationManager locationManager;
    MapboxMap mapboxMap;
    private Handler handler;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private CardView goToMap;
    private SwitchCompat mapToggleLightDark;
    private Style.OnStyleLoaded onStyleLoaded;
    private TextView yourlocationtxt;
    private boolean flagMapTheme;
    private MarkerOptions markerOptions2;
    private MarkerOptions markerOptions1;
    private Icon icon1;
    private Bitmap bitmap1;
    private Icon icon2;
    private Bitmap scaledBitmap2;
    private Bitmap bitmap2;
    private Marker userMarker1;
    private Marker userMarker2;
    private boolean markersAdded = false;
    //    private View view;
    private int currentSize;
    private final int MIN_SIZE = 250;
    private boolean styleIsLoaded = false;

    //--------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Mapbox.getInstance(getActivity(), getResources().getString(R.string.accessToken));
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);

//        Window window = getActivity().getWindow();
//        window.setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.main_500));

        dialogPlus = DialogPlus.newDialog(getActivity())
                .setContentHolder(new ViewHolder(R.layout.custom_loading_dialog))
                .setContentBackgroundResource(Color.TRANSPARENT)
                .setGravity(Gravity.CENTER)
                .create();

        dialogPlus.show();

        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        fromLocLayout = view.findViewById(R.id.fromLocLayout);
        toLocLayout =view.findViewById(R.id.toLocLL);

        fromLocIcon = view.findViewById(R.id.fromLocIcon);
        toLocIcon = view.findViewById(R.id.toLocIcon);

        params1 = (LinearLayout.LayoutParams) fromLocLayout.getLayoutParams();
        params2 = (LinearLayout.LayoutParams) toLocLayout.getLayoutParams();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(false);

        recyclerIndicator = view.findViewById(R.id.indicator);
        recyclerIndicator.setDotColor(getResources().getColor(R.color.medium_grey));
        recyclerIndicator.setSelectedDotColor(getResources().getColor(R.color.dark_grey));


        SearchBus =  view.findViewById(R.id.SearchBus);

        btn_swap = view.findViewById(R.id.btn_swap);

//        layout = view.findViewById(R.id.FDLLM);

//        recyclerView = view.findViewById(R.id.Bus_recyclerView);

        databaseReference = FirebaseDatabase.getInstance().getReference("Buses");

//        busList = new ArrayList<>();

//        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

//        busAdapter = new BusAdapter(busList, getActivity());

        sendingData = new ProgressDialog(getActivity());
        sendingData.setTitle("Saving data to database");
        sendingData.setCancelable(false);

//        recyclerView.setAdapter(busAdapter);

        runAutoScroll();

        initialize();
        FirebaseDataRetrieve();
        spinner();

        btn_swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                vibrator.vibrate(300);

                if (!swapped){
                    fromLocLayout.removeView(fromLocIcon);
                    toLocLayout.addView(fromLocIcon);
                    toLocLayout.removeView(toLocIcon);
                    fromLocLayout.addView(toLocIcon);

                    fromLocLayout.removeView(FromLocation);
                    toLocLayout.addView(FromLocation);
                    toLocLayout.removeView(ToLocation);
                    fromLocLayout.addView(ToLocation);

                    swapLinearLayouts(fromLocLayout, toLocLayout);
                }else{
                    fromLocLayout.removeView(toLocIcon);
                    toLocLayout.addView(toLocIcon);
                    toLocLayout.removeView(fromLocIcon);
                    fromLocLayout.addView(fromLocIcon);

                    fromLocLayout.removeView(ToLocation);
                    toLocLayout.addView(ToLocation);
                    toLocLayout.removeView(FromLocation);
                    fromLocLayout.addView(FromLocation);

                    swapLinearLayouts(toLocLayout, fromLocLayout);
                }
                swapped = !swapped;

                ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(btn_swap, "rotation", 0f, 180f);
                rotateAnimator.setDuration(300);
                rotateAnimator.start();



            }
        });

        SearchBus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String dbLocationDeparture = fromLocation;
                String dbLocationDestination = toLocation;


                if (!dbLocationDeparture.equals(dbLocationDestination)){

                    Intent iBusSearch = new Intent(getActivity(), BusSearch.class);
                    iBusSearch.putExtra("fromLocation",fromLocation);
                    iBusSearch.putExtra("toLocation",toLocation);
                    startActivity(iBusSearch);

                }else {
                    Toast.makeText(getActivity(), "Locations cannot be same", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }


    public void onMapReady(@NonNull MapboxMap mapboxMap1) {

        mapboxMap= mapboxMap1;

        onStyleLoaded = new Style.OnStyleLoaded() {

            @Override
            public void onStyleLoaded(@NonNull Style style) {
                styleIsLoaded = true;

                CameraPosition position = new CameraPosition.Builder()
                        .target(currentLatLng)
                        .zoom(13)
                        .padding(0, 0, 0, 50)
                        .build();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600, new MapboxMap.CancelableCallback() {
                            @Override
                            public void onCancel() {}

                            @Override
                            public void onFinish() {
                                userCurrentLocationIcon();
                            }
                        });
                    }
                },10000);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        userCurrentLocationIcon();
                    }
                },1000);

            }
        };

        mapboxMap.setStyle(Style.MAPBOX_STREETS,onStyleLoaded);
    }


//    @Override
//    public void onPause() {
//        super.onPause();
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }

//    @Override
//    public void onPause() {
//        super.onPause();
//
//        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
//        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//
//        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            if (isLocationEnabled) {
//                fusedLocationClient.removeLocationUpdates(locationCallback);
//            } else {
//                Toast.makeText(requireContext(), "Please turn on location services!", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            Toast.makeText(requireContext(), "Location permission is required for Map!", Toast.LENGTH_SHORT).show();
//        }
//
//        mapView.onPause();
//    }


    private void fetchLocation() {

        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

        {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        else {

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        for (Location location : locationResult.getLocations()) {

                            if (currentLatLng == null) {
                                currentLatLng = new LatLng(0, 0);
                            }
                            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            if (mapboxMap != null) {

                                CameraPosition position = new CameraPosition.Builder()
                                        .target(currentLatLng)
                                        .zoom(13)
                                        .padding(0, 0, 0, 50)
                                        .build();

                                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                            }
                        }
                    }
                };
                fusedLocationClient.requestLocationUpdates(new LocationRequest().setInterval(0), locationCallback, Looper.myLooper());
            }
        }
    }


    private void userCurrentLocationIcon() {
        if(isAdded()) {
            if (mapboxMap != null) {
                Drawable drawable1 = ContextCompat.getDrawable(getActivity(), R.drawable.icon_circle_outline);
                Drawable drawable2 = ContextCompat.getDrawable(getActivity(), R.drawable.icon_circle_outline2);

                float density = getResources().getDisplayMetrics().density;

                Bitmap bitmap1 = BitmapUtils.getBitmapFromDrawable(drawable1);
                Icon icon1 = IconFactory.getInstance(getActivity()).fromBitmap(Objects.requireNonNull(bitmap1));

                if (!markersAdded) {
                    if (currentLatLng != null) {
                        MarkerOptions markerOptions1 = new MarkerOptions().position(currentLatLng).icon(icon1);
                        mapboxMap.addMarker(markerOptions1);

                        bitmap2 = BitmapUtils.getBitmapFromDrawable(drawable2);
                        currentSize = (int) (50 * density * (1 - (mapboxMap.getCameraPosition().zoom - 14) / 4));
                        currentSize = Math.max(currentSize, MIN_SIZE);
                        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap2, currentSize, currentSize, false);
                        icon2 = IconFactory.getInstance(getActivity()).fromBitmap(newBitmap);

                        MarkerOptions markerOptions2 = new MarkerOptions().position(currentLatLng).icon(icon2);
                        userMarker2 = mapboxMap.addMarker(markerOptions2);

                        markersAdded = true;

                        mapboxMap.addOnCameraMoveListener(() -> {
                            int currentSize1 = (int) (50 * density * (1 - (mapboxMap.getCameraPosition().zoom - 14) / 4));
                            currentSize1 = Math.max(currentSize1, MIN_SIZE);

                            if (currentSize != currentSize1) {
                                currentSize = currentSize1;
                                Bitmap newBitmap1 = Bitmap.createScaledBitmap(bitmap2, currentSize1, currentSize1, false);
                                Icon newIcon = IconFactory.getInstance(getActivity()).fromBitmap(newBitmap1);
                                userMarker2.setIcon(newIcon);
                            }
                        });

                    }
                }

            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /*LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled) {
                fusedLocationClient.requestLocationUpdates(new LocationRequest().setInterval(0), locationCallback, null);
            } else {
                Toast.makeText(requireContext(), "Please turn on location services!", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(
                    getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            Toast.makeText(requireContext(), "Location permission is required for Map!", Toast.LENGTH_SHORT).show();
        }

        userCurrentLocationIcon();*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.requestLocationUpdates(new LocationRequest().setInterval(0), locationCallback, null);
                }
            }
        }
    }

    private void swapLinearLayouts(LinearLayout fromLocLayout, LinearLayout toLocLayout) {

        int spinner1Index = FromLocation.getSelectedItemPosition();
        FromLocation.setSelection(ToLocation.getSelectedItemPosition());
        ToLocation.setSelection(spinner1Index );

        float fromY = fromLocLayout.getY();
        float toY = toLocLayout.getY();

        // Calculate the translation distances
        float fromTranslation = toY - fromY;
        float toTranslation = fromY - toY;

        // Animate the LinearLayouts to swap places
        fromLocLayout.animate().translationYBy(fromTranslation).setDuration(300).withEndAction(() -> {
            btn_swap.setEnabled(true);
        }).start();
        toLocLayout.animate().translationYBy(toTranslation).setDuration(300).withEndAction(() -> {
            btn_swap.setEnabled(true);
        }).start();

        // Disable the button until the animation finishes
        btn_swap.setEnabled(false);

        // Update the LayoutParams of the LinearLayouts
        ViewGroup.LayoutParams fromParams = fromLocLayout.getLayoutParams();
        ViewGroup.LayoutParams toParams = toLocLayout.getLayoutParams();
        fromLocLayout.setLayoutParams(toParams);
        toLocLayout.setLayoutParams(fromParams);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            iFirebaseLoadDone = (IFirebaseLoadDone ) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity
                    + " must implement MyInterface ");
        }
    }

    private void initialize() {

        //------------------------
        context = getContext();
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        //------------------------

        FromLocation = view.findViewById(R.id.StartLocationAB);
        ToLocation = view.findViewById(R.id.ToLocationAB);

        databaseReference = FirebaseDatabase.getInstance().getReference("Buses");

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        busPassModels = new ArrayList<>();
        busPassModels.add(new BusPassModel(R.drawable.one_month_card,"Daily","daily"));
        busPassModels.add(new BusPassModel(R.drawable.one_month_card, "1\nMonth", "1 Month"));
        busPassModels.add(new BusPassModel(R.drawable.three_month_card, "3\nMonth", "3 Month"));
        busPassModels.add(new BusPassModel(R.drawable.six_month_card, "6\nMonth", "6 Month"));
        busPassModels.add(new BusPassModel(R.drawable.twelve_month_card, "12\nMonth", "12 Month"));

        Map<String, String> benefitsMap = new HashMap<>();
        benefitsMap.put("Benefit1", "All the benefits of the 6-month pass");
        benefitsMap.put("Benefit2", "Provides a hassle-free transportation option for a full year");
        benefitsMap.put("Benefit3", "40% discount on the regular price of a pass");
        benefitsMap.put("Benefit4", "Offers the most significant savings compared to any other pass option over the couse of a year");
        benefitsMap.put("Benefit5", "Freedom to use the bus whenever you want without worrying about the cost");
        benefitsMap.put("Benefit6", "Ideal for those who use the bus daily for work, school, or other activities");
        benefitsMap.put("Benefit7", "Great option for professionals or employees in the same location for a year, as it eliminates the need to constantly renew their pass.");
        benefitsMap.put("Benefit8", "Best value for your money");
        benefitsMap.put("Benefit9", "Offers predictability and stability in transportation expenses, allowing users to plan\n" +
                "\n" +
                "their budget more effectively for the upcoming year");
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("BusPass");
        BusPassCreationModel dailyPlan = new BusPassCreationModel("1 Day", "70", benefitsMap);
        databaseReference.child("daily").setValue(dailyPlan);
        BusPassCreationModel oneMonthPlan = new BusPassCreationModel("1 Month(31 Days)", "1050", benefitsMap);
        databaseReference.child("1Month").setValue(oneMonthPlan);
        BusPassCreationModel threeMonthPlan = new BusPassCreationModel("3 Month(90 Days)", "1300", benefitsMap);
        databaseReference.child("3Month").setValue(threeMonthPlan);
        BusPassCreationModel sixMonthPlan = new BusPassCreationModel("6 Month(180 Days)", "1500", benefitsMap);
        databaseReference.child("6Month").setValue(sixMonthPlan);
        BusPassCreationModel oneYearPlan = new BusPassCreationModel("12 Month(365 Days)", "1800", benefitsMap);
        databaseReference.child("12Month").setValue(oneYearPlan);
        DatabaseReference databaseBusReference = FirebaseDatabase.getInstance().getReference("Buses");
        // Adding sample bus items
        List<BusItem> busList = generateSampleBusData();

        // Push the data to the "bus" collection in Firebase
        for (BusItem bus : busList) {
            databaseBusReference.child(bus.getFromLocation() + bus.getToLocation()).child(bus.getBusNo()).setValue(bus);
        }

        DatabaseReference databaseLocationReference = FirebaseDatabase.getInstance().getReference("Locations");

        // Sample data for locations in Bangalore
        List<LocationItem> locationList = generateSampleLocationData();

        // Push the data to the "locations" collection in Firebase
        for (LocationItem location : locationList) {
            databaseLocationReference.child(location.getPlace()).setValue(location);
        }

        busPassAdapter = new BusPassAdapter(busPassModels, getActivity());
        recyclerView.setAdapter(busPassAdapter);
        recyclerView.setPadding(100,0,100,0);

        recyclerIndicator.attachToRecyclerView(recyclerView);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder viewHoler = recyclerView.findViewHolderForAdapterPosition(pos);
                RelativeLayout rl1 = viewHoler.itemView.findViewById(R.id.rL1);
                rl1.animate().setDuration(350).scaleX(1).scaleY(1).setInterpolator(new AccelerateInterpolator()).start();
            }
        },100);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                paused = true;

                View v = snapHelper.findSnapView(layoutManager);
                pos = layoutManager.getPosition(v);

                switch (pos){

                    case 0:
                        recyclerIndicator.setDotColor(getResources().getColor(R.color.medium_grey));
                        recyclerIndicator.setSelectedDotColor(getResources().getColor(R.color.dark_grey));
                        break;
                    case 1:
                        recyclerIndicator.setDotColor(getResources().getColor(R.color.buspass_green));
                        recyclerIndicator.setSelectedDotColor(getResources().getColor(R.color.buspass_blue));

                        break;
                    case 2:
                        recyclerIndicator.setDotColor(getResources().getColor(R.color.buspass_pink));
                        recyclerIndicator.setSelectedDotColor(getResources().getColor(R.color.buspass_orange));

                        break;
                    case 3:
                        recyclerIndicator.setDotColor(getResources().getColor(R.color.buspass_purple));
                        recyclerIndicator.setSelectedDotColor(getResources().getColor(R.color.buspass_endpink));

                        break;

                }

                RecyclerView.ViewHolder viewHoler = recyclerView.findViewHolderForAdapterPosition(pos);
                RelativeLayout rl1 = viewHoler.itemView.findViewById(R.id.rL1);

                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    paused = false;
                    rl1.animate().setDuration(350).scaleX(1).scaleY(1).setInterpolator(new AccelerateInterpolator()).start();
                }else{
                    rl1.animate().setDuration(350).scaleX(0.9f).scaleY(0.9f).setInterpolator(new AccelerateInterpolator()).start();

                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

    }

    private void spinner() {

        FromLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                IDs iD = iDs.get(i);
                fromLocation = iD.getPlace();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        ToLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                IDs iD = iDs.get(position);
                toLocation = iD.getPlace();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    private static List<BusItem> generateSampleBusData() {
        List<BusItem> busList = List.of(
                new BusItem("ABC123", "25", "Electronic City", "Koramangala", "08:00", "09:30"),
                new BusItem("XYZ456", "25", "Whitefield", "Indiranagar", "09:30", "11:00"),
                new BusItem("PQR789", "25", "Marathahalli", "Jayanagar", "10:00", "12:00"),
                new BusItem("MNO321", "20", "BTM Layout", "Yeshwantpur", "11:30", "13:00"),
                new BusItem("JKL654", "25", "Hebbal", "Malleshwaram", "13:00", "14:30"),
                new BusItem("DEF987", "20", "Silk Board", "Shivajinagar", "15:00", "16:30"),
                new BusItem("GHI654", "25", "KR Puram", "Basavanagudi", "17:00", "18:30"),
                new BusItem("STU321", "25", "Hosur Road", "Vijayanagar", "19:30", "21:00"),
                new BusItem("VWX987", "25", "Banashankari", "Peenya", "22:00", "23:30"),
                new BusItem("PMN501", "20", "Malleshwaram", "PES Bus Stop", "10:00", "20:00")
        );

        return busList;
    }

    private static List<LocationItem> generateSampleLocationData() {
        List<LocationItem> locationList = List.of(
                new LocationItem("PIN001", "Electronic City"),
                new LocationItem("PIN002", "Whitefield"),
                new LocationItem("PIN003", "Marathahalli"),
                new LocationItem("PIN004", "BTM Layout"),
                new LocationItem("PIN005", "Hebbal"),
                new LocationItem("PIN006", "Silk Board"),
                new LocationItem("PIN007", "KR Puram"),
                new LocationItem("PIN008", "Hosur Road"),
                new LocationItem("PIN009", "Banashankari"),
                new LocationItem("PIN010", "Malleshwaram"),
                new LocationItem("PIN011", "Vijayanagar"),
                new LocationItem("PIN012", "Jayanagar"),
                new LocationItem("PIN013", "Basavanagudi"),
                new LocationItem("PIN014", "Yeshwantpur"),
                new LocationItem("PIN015", "Peenya"),
                new LocationItem("PIN016", "Shivajinagar"),
                new LocationItem("PIN014", "Indiranagar"),
                new LocationItem("PIN015", "PES Bus Stop")
        );

        return locationList;
    }

    private void FirebaseDataRetrieve() {

        DatabaseReference locationRef = FirebaseDatabase.getInstance().getReference("Locations");
        iFirebaseLoadDone = this;
        locationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<IDs> iDs = new ArrayList<>();

                for (DataSnapshot idSnapShot:dataSnapshot.getChildren()){
                    iDs.add(idSnapShot.getValue(IDs.class));
                }
                iFirebaseLoadDone.onFirebaseLoadSuccess(iDs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                iFirebaseLoadDone.onFirebaseLoadFailed(databaseError.getMessage());
            }
        });
    }

    @Override
    public void onFirebaseLoadSuccess(List<IDs> LocationList) {

        iDs = LocationList;
        List<String> id_list = new ArrayList<>();

        for (IDs id : LocationList){
            id_list.add(id.getPlace());
            adapterFrom = new ArrayAdapter<>(getActivity(), R.layout.spinner_item_list_1, id_list);

            adapterTo = new ArrayAdapter<>(getActivity(), R.layout.spinner_item_list_1, id_list);

            FromLocation.setAdapter(adapterFrom);
            ToLocation.setAdapter(adapterTo);
            if (id_list.size() > 1){
                ToLocation.setSelection(1);
            }
        }
        dialogPlus.dismiss();
    }

    @Override
    public void onFirebaseLoadFailed(String Message) {

        progressDialog.dismiss();
        Toast.makeText(getActivity(), Message, Toast.LENGTH_LONG).show();
    }

    public void runAutoScroll(){

        if (!paused){
            if (timer == null && timerTask == null){

                timer = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {

                        if (getActivity() != null){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (pos == 3){
                                        pos = 0;
                                        recyclerView.smoothScrollToPosition(pos);

                                    }else{
                                        pos++;
                                        recyclerView.smoothScrollToPosition(pos);
                                    }
                                }
                            });
                        }
                    }
                };
                timer.schedule(timerTask, 10000, 10000);
            }
        }
    }



}