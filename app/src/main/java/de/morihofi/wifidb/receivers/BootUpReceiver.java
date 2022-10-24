package de.morihofi.wifidb.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import de.morihofi.wifidb.activities.MainActivity;

public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //PendingIntent pi = PendingIntent.getService(context, 0, new Intent(context, MyService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        //am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pi);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if(preferences.getBoolean("autostart",false)){
            MainActivity.startWifiCollectingService(context,preferences);
        }

    }
}