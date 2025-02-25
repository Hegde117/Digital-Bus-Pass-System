package com.example.ticketbus;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wajahatkarim3.easyflipview.EasyFlipView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserBusPass extends AppCompatActivity {

    // Declare UI components
    public Point point;
    public ImageView BusPassQR;
    public EasyFlipView flipView;
    public WindowManager manager;
    public Display display;
    private TextView username;
    private TextView txt_busPassId;
    private static TextView txt_daysleft;
    private TextView txt_buspassvaliddate;
    private TextView txt_buspassexpirydate;
    private CircleImageView userProfilePic;
    private LinearLayout busPassFrontCard, busPassBackCard;
    public ShimmerFrameLayout shimmerContainer;

    // Declare Firebase components
    private DatabaseReference dbreference1, dbreference2;
    private String userId;
    private String userName;
    private static String str_DaysLeftTillExpiry;
    private String str_ValidDateTime;
    private String str_currentDateTime;
    private String str_expiryDateTime;
    private String str_busPassID, str_busPassType, str_data;

    // Declare handlers and formatter
    private Handler mHandler;
    private static final int UPDATE_INTERVAL = 24 * 60 * 60 * 1000;
    private DateTimeFormatter formatter;

    // Declare date and time variables
    private LocalDateTime localDateTime, expiryLocalDateTime;
    private Button btn_buyBusPassAgain;
    private TextView expiredtext;
    private Vibrator vibrator;

    ImageButton back_bus_pass;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_bus_pass);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onStart() {
        super.onStart();

        Initalise();
        swipe();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void Initalise() {

        back_bus_pass = findViewById(R.id.btn_back_user_pass);

        back_bus_pass.setOnClickListener(view -> finish());

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        userId = currentUser.getUid();

        flipView = findViewById(R.id.flipView);
        BusPassQR = findViewById(R.id.budPassQr);
        username = findViewById(R.id.txt_userName);
        txt_busPassId = findViewById(R.id.txt_busPassId);
        userProfilePic = findViewById(R.id.userProfilePic);
        txt_daysleft = findViewById(R.id.txt_daysleft);
        txt_buspassvaliddate = findViewById(R.id.buspassvaliddate);
        txt_buspassexpirydate = findViewById(R.id.buspassexpirydate);
        busPassFrontCard = findViewById(R.id.busPassFrontCard);
        busPassBackCard = findViewById(R.id.busPassBackCard);
        btn_buyBusPassAgain = findViewById(R.id.BuyBusPassAgain);
        expiredtext = findViewById(R.id.expiredtext);

        shimmerContainer = findViewById(R.id.shimmer_view_container);
        shimmerContainer.showShimmer(true);
        shimmerContainer.startShimmer();

        formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss", Locale.ENGLISH);

        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        display = manager.getDefaultDisplay();
        point = new Point();
        display.getSize(point);

        dbreference1 = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(userId);

        dbreference2 = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(userId)
                .child("BusPass");
        fetch();
        boolean alarmAlreadyScheduled = isAlarmAlreadyScheduled();

        // If the alarm has not been scheduled yet, schedule it
        if (!alarmAlreadyScheduled) {
            scheduleDailyAlarm();
            // Mark the alarm as scheduled in shared preferences
            setAlarmScheduledFlag(true);
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    public void swipe() {

        flipView.setOnTouchListener(new SwipeDetector(this, new SwipeDetector.OnSwipeListener() {
            @Override
            public void onSwipeLeftToRight() {
                flipView.setFlipDuration(750);
                flipView.setFlipTypeFromRight();
                flipView.flipTheView();

            }

            @Override
            public void onSwipeRightToLeft() {
                flipView.setFlipDuration(750);
                flipView.setFlipTypeFromLeft();
                flipView.flipTheView();

            }
        }));
    }


    public void fetch(){

        dbreference2.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                str_busPassID = (snapshot.child("BusPassId").getValue()).toString();
                str_DaysLeftTillExpiry = (snapshot.child("DaysLeftTillExpiry").getValue()).toString();
                str_ValidDateTime = (snapshot.child("ValidDateTime").getValue()).toString();
                str_expiryDateTime = (snapshot.child("ExpiryDateTime").getValue()).toString();
                str_busPassType = (snapshot.child("BusPassType").getValue()).toString();
                str_data = (snapshot.child("BusPassQRCode").getValue()).toString();

                localDateTime = LocalDateTime.now();
                str_currentDateTime = localDateTime.format(formatter);
                expiryLocalDateTime = LocalDateTime.parse(str_expiryDateTime, formatter);

                txt_busPassId.setText(str_busPassID);
                txt_daysleft.setText(String.format("%s Days Left", str_DaysLeftTillExpiry));
                txt_buspassvaliddate.setText(str_ValidDateTime);
                txt_buspassexpirydate.setText(str_expiryDateTime);
                shimmerContainer.stopShimmer();
                shimmerContainer.hideShimmer();
                passCardColor();
                regenerate();

            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbreference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userName = snapshot.child("Name").getValue(String.class);
                username.setText(userName);

                String profilePicture = snapshot.child("ProfilePicture").getValue(String.class);

                if (profilePicture != null && !profilePicture.equals("null")) {
                    Glide.with(getApplicationContext()).load(profilePicture).into(userProfilePic);
                }
                else {
                    userProfilePic.setImageResource(R.drawable.bmtc_logo);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}

        });
    }

    private void scheduleDailyAlarm() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Set the hour to trigger the alarm
        calendar.set(Calendar.MINUTE, 0); // Set the minute to trigger the alarm

        Intent intent = new Intent(this, AlaramReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            // Set the alarm to trigger daily
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void updateDaysLeftAndRefreshUI(Activity activity, DatabaseReference dbreference) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Fetch the expiry date from the database
        dbreference.child("ExpiryDateTime").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String expiryDateString = snapshot.getValue(String.class);
                if (expiryDateString != null) {
                    // Parse the expiry date
                    LocalDate expiryDate = LocalDate.parse(expiryDateString, DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"));

                    // Calculate the days left
                    long daysLeft = ChronoUnit.DAYS.between(currentDate, expiryDate);

                    // Update the days left in the database
                    dbreference.child("DaysLeftTillExpiry").setValue(daysLeft);

                    // Refresh the UI to display the updated days left
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView txt_daysleft = activity.findViewById(R.id.txt_daysleft);
                            if (txt_daysleft != null) {
                                txt_daysleft.setText(String.format("%s Days Left", daysLeft));
                            } else {
                                Log.e(TAG, "TextView txt_daysleft not found in activity.");
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to get expiry date from the database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read value.", error.toException());
            }
        });
    }




    private void passCardColor() {
        switch (str_busPassType) {
            case "1Month": {
                Drawable drawablea = ContextCompat.getDrawable(getApplicationContext(), R.drawable.one_month_pass_bg_front);
                Drawable drawableb = ContextCompat.getDrawable(getApplicationContext(), R.drawable.one_month_pass_bg_back);
                busPassFrontCard.setBackground(drawablea);
                busPassBackCard.setBackground(drawableb);
                break;
            }
            case "3Month": {
                Drawable drawablea = ContextCompat.getDrawable(getApplicationContext(), R.drawable.three_month_pass_bg_front);
                Drawable drawableb = ContextCompat.getDrawable(getApplicationContext(), R.drawable.three_month_pass_bg_back);
                busPassFrontCard.setBackground(drawablea);
                busPassBackCard.setBackground(drawableb);
                break;
            }
            case "6Month": {
                Drawable drawablea = ContextCompat.getDrawable(getApplicationContext(), R.drawable.six_month_pass_bg_front);
                Drawable drawableb = ContextCompat.getDrawable(getApplicationContext(), R.drawable.six_month_pass_bg_back);
                busPassFrontCard.setBackground(drawablea);
                busPassBackCard.setBackground(drawableb);
                break;
            }
            case "12Month": {
                Drawable drawablea = ContextCompat.getDrawable(getApplicationContext(), R.drawable.twelve_month_pass_bg_front);
                Drawable drawableb = ContextCompat.getDrawable(getApplicationContext(), R.drawable.twelve_month_pass_bg_back);
                busPassFrontCard.setBackground(drawablea);
                busPassBackCard.setBackground(drawableb);
                break;
            }
        }
    }

    public void daysLeftIsZero() {
        expiredtext.setVisibility(View.VISIBLE);
        shimmerContainer.setAlpha(.5f);
        flipView.setFlipEnabled(false);
        txt_busPassId.setText("");
        dbreference2.child("isExpired").setValue("true");
        btn_buyBusPassAgain.setVisibility(View.VISIBLE);
        btn_buyBusPassAgain.setEnabled(false);
        btn_buyBusPassAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbreference2.removeValue();
                Intent i = new Intent(getApplicationContext(), CreateBusPassActivity.class);
                startActivity(i);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getDaysLeft(){
        Duration duration = Duration.between(localDateTime, expiryLocalDateTime);
        long daysLeft = Long.parseLong(String.valueOf(duration.toDays())) + 1;

        if(daysLeft <0)
            daysLeft =0;

        if(daysLeft == 0){
            daysLeftIsZero();
        }
        else{
            expiredtext.setVisibility(View.INVISIBLE);
            shimmerContainer.setAlpha(1);
            flipView.setFlipEnabled(true);
            dbreference2.child("isExpired").setValue("false");
            btn_buyBusPassAgain.setEnabled(false);
            btn_buyBusPassAgain.setVisibility(View.GONE);
        }

        dbreference2.child("DaysLeftTillExpiry").setValue(daysLeft);
    }

    public void generateQRCode(){

        dbreference2.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                getDaysLeft();

                if(snapshot.hasChild("isExpired")){

                    boolean isExpired =Boolean.parseBoolean(Objects.requireNonNull(snapshot.child("isExpired").getValue()).toString());

                    if(!isExpired){

                        String QRCode = String.join("," , "TicketBusPass", str_busPassID, userId, str_busPassType, str_expiryDateTime, str_currentDateTime);
                        dbreference2.child("BusPassQRCode").setValue(QRCode);
                        dbreference2.child("BusPassId").setValue(str_busPassID);

                        try {
                            QRCodeWriter qrCodeWriter = new QRCodeWriter();
                            BitMatrix bitMatrix = qrCodeWriter.encode(str_data, BarcodeFormat.QR_CODE, 400, 400);
                            Bitmap bitmap = Bitmap.createBitmap(bitMatrix.getWidth(), bitMatrix.getHeight(), Bitmap.Config.ARGB_8888);
                            bitmap.eraseColor(Color.TRANSPARENT);

                            int width = bitMatrix.getWidth();
                            int height = bitMatrix.getHeight();
                            for (int x = 0; x < width; x++) {
                                for (int y = 0; y < height; y++) {
                                    int pixelColor = bitMatrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                                    bitmap.setPixel(x, y, pixelColor);
                                }
                            }
                            BusPassQR.setImageBitmap(bitmap);
                        }
                        catch (WriterException e) {
                            Log.e(TAG, "Failed to generate QR code", e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void regenerate() {
        mHandler = new Handler();

        Runnable mRunnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                generateQRCode();
                mHandler.postDelayed(this, 600000);
            }
        };
        mHandler.postDelayed(mRunnable, 0);

        Runnable mRunnable2 = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                getDaysLeft();
                mHandler.postDelayed(this, 10000);
            }
        };
        mHandler.postDelayed(mRunnable2, 0);
    }

    private boolean isAlarmAlreadyScheduled() {
        // Check the flag in shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("AlarmScheduled", false);
    }

    private void setAlarmScheduledFlag(boolean alarmScheduled) {
        // Set the flag in shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("AlarmScheduled", alarmScheduled);
        editor.apply();
    }

//    @Override
//    public void onBackPressed() {
//
//        FragmentManager fragmentManager=getSupportFragmentManager();
//        int count=fragmentManager.getBackStackEntryCount();
//        FragmentTransaction transaction=fragmentManager.beginTransaction();
//
//        if (count > 0) {
//            fragmentManager.popBackStackImmediate();
//        }
//        else {
//            FragmentProfileMain fragmentProfileMain = new FragmentProfileMain();
//            transaction.replace(R.id.rootUserProfileLayout,fragmentProfileMain);
//            transaction.addToBackStack(null);
//            transaction.commit();
//            super.onBackPressed();
//            finish();
//        }
//    }


}