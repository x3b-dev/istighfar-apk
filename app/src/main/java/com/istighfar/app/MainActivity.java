package com.istighfar.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_NOTIF = 77;
    private static final int REQ_LOC = 78;
    private WebView web;
    private Updater updater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }

        web = new WebView(this);
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        web.setBackgroundColor(night ? 0xFF0B1820 : 0xFFF3EFE6);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setSupportZoom(false);

        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        updater = new Updater(this, web);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        hideBars();

        // Re-arm the daily reminders (keeps them correct after time-zone changes etc.)
        if (Reminders.isEnabled(this)) Reminders.scheduleAll(this);
        try { Prayers.scheduleAll(this); } catch (Exception ignored) { }
    }

    // ---- location -------------------------------------------------------
    private void locFail(String why) {
        final String js = "window.__locResult&&window.__locResult(null," + JSONObject.quote(why) + ")";
        runOnUiThread(new Runnable() {
            @Override public void run() { if (web != null) web.evaluateJavascript(js, null); }
        });
    }

    private void locOk(final Location l) {
        final double lat = l.getLatitude(), lng = l.getLongitude();
        new Thread(new Runnable() {
            @Override public void run() {
                String gov = "", dist = "", city = "", country = "";
                try {
                    if (Geocoder.isPresent()) {
                        Geocoder g = new Geocoder(MainActivity.this, new Locale("ar"));
                        List<Address> a = g.getFromLocation(lat, lng, 1);
                        if (a != null && !a.isEmpty()) {
                            Address x = a.get(0);
                            gov = nz(x.getAdminArea());
                            dist = nz(x.getSubAdminArea());
                            city = nz(x.getLocality());
                            country = nz(x.getCountryName());
                        }
                    }
                } catch (Exception ignored) { }
                String js;
                try {
                    JSONObject o = new JSONObject();
                    o.put("lat", lat); o.put("lng", lng);
                    o.put("gov", gov); o.put("dist", dist);
                    o.put("city", city); o.put("country", country);
                    js = "window.__locResult&&window.__locResult(" + o.toString() + ",null)";
                } catch (Exception e) { js = "window.__locResult&&window.__locResult(null,'err')"; }
                final String fjs = js;
                runOnUiThread(new Runnable() {
                    @Override public void run() { if (web != null) web.evaluateJavascript(fjs, null); }
                });
            }
        }).start();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void fetchLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { locFail("nolm"); return; }
        Location best = null;
        try {
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (SecurityException e) { locFail("perm"); return; }
        if (best != null && System.currentTimeMillis() - best.getTime() < 6L * 3600 * 1000) {
            locOk(best);
            return;
        }
        String prov = null;
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) prov = LocationManager.NETWORK_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) prov = LocationManager.GPS_PROVIDER;
        } catch (Exception ignored) { }
        if (prov == null) {
            if (best != null) locOk(best); else locFail("off");
            return;
        }
        final Location fallback = best;
        final LocationManager flm = lm;
        final boolean[] done = {false};
        final LocationListener ll = new LocationListener() {
            @Override public void onLocationChanged(Location l) {
                if (done[0]) return; done[0] = true; locOk(l);
            }
            @Override public void onStatusChanged(String p, int s, android.os.Bundle b) { }
            @Override public void onProviderEnabled(String p) { }
            @Override public void onProviderDisabled(String p) { }
        };
        try {
            lm.requestSingleUpdate(prov, ll, Looper.getMainLooper());
        } catch (SecurityException e) { locFail("perm"); return; }
        web.postDelayed(new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                try { flm.removeUpdates(ll); } catch (Exception ignored) { }
                if (fallback != null) locOk(fallback); else locFail("timeout");
            }
        }, 25000);
    }

    private boolean systemNotificationsAllowed() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        return nm == null || nm.areNotificationsEnabled();
    }

    private void tellPage(boolean on) {
        if (web != null) {
            web.evaluateJavascript("window.__notifResult&&window.__notifResult(" + on + ")", null);
        }
    }

    private void enableReminders() {
        if (!systemNotificationsAllowed()) {
            Reminders.setEnabled(this, false);
            Toast.makeText(this, "الإشعارات مغلقة لهذا التطبيق من إعدادات الهاتف", Toast.LENGTH_LONG).show();
            tellPage(false);
            return;
        }
        Reminders.setEnabled(this, true);
        Toast.makeText(this, "تم تفعيل التذكير اليومي بالاستغفار", Toast.LENGTH_SHORT).show();
        tellPage(true);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_LOC) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) fetchLocation();
            else locFail("denied");
            return;
        }
        if (code == REQ_NOTIF) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                enableReminders();
            } else {
                Reminders.setEnabled(this, false);
                tellPage(false);
            }
        }
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            finish();
            return;
        }
        web.evaluateJavascript("(window.__back&&window.__back())?'1':'0'", value -> {
            if (!"\"1\"".equals(value)) {
                String u = web == null ? null : web.getUrl();
                if (u != null && !u.contains("/index.html")) web.loadUrl("file:///android_asset/index.html");
                else finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Called from the page: Android.share("text"), Android.setNotifications(bool), Android.notifEnabled() */
    private class Bridge {
        @JavascriptInterface
        public boolean notifEnabled() {
            return Reminders.isEnabled(MainActivity.this) && systemNotificationsAllowed();
        }

        @JavascriptInterface
        public void setNotifications(final boolean on) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!on) {
                        Reminders.setEnabled(MainActivity.this, false);
                        tellPage(false);
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 33
                            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                        return;
                    }
                    enableReminders();
                }
            });
        }

        @JavascriptInterface
        public void requestLocation() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
                    } else {
                        fetchLocation();
                    }
                }
            });
        }

        /** json: [{"t":epochMillis,"n":"الفجر"},...] ; "[]" cancels all prayer alarms. */
        @JavascriptInterface
        public void setPrayerAlarms(final String json) {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 79);
                    }
                });
            }
            Prayers.save(MainActivity.this, json);
        }

        @JavascriptInterface
        public boolean systemDark() {
            return (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        @JavascriptInterface
        public void setBarColor(final boolean dark) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (web != null) web.setBackgroundColor(dark ? 0xFF0B1820 : 0xFFF3EFE6);
                }
            });
        }

        @JavascriptInterface
        public void checkUpdate() { updater.check(); }

        @JavascriptInterface
        public void installUpdate() {
            runOnUiThread(new Runnable() { @Override public void run() { updater.download(); } });
        }

        @JavascriptInterface
        public String appVersion() { return updater.currentName(); }

        @JavascriptInterface
        public void share(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, null));
                }
            });
        }
    }
}
