package com.example.ticketbus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AlaramReceiver extends BroadcastReceiver {

    private DatabaseReference dbreference2;
    @Override
    public void onReceive(Context context, Intent intent) {
        // Perform background task to update the days left
        // UpdateDaysLeftTask.execute();
        // Send broadcast to refresh UI
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        dbreference2 = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(currentUser.getUid())
                .child("BusPass");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UserBusPass.updateDaysLeftAndRefreshUI((Activity) context,dbreference2);
        }
    }
}
