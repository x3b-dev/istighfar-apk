package com.istighfar.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** In-app updater: asks the GitHub "latest release", downloads Istighfar.apk, hands it to the installer. */
public class Updater {

    static final String API = "https://api.github.com/repos/x3b-dev/istighfar-apk/releases/latest";
    static final String APK_URL = "https://github.com/x3b-dev/istighfar-apk/releases/latest/download/Istighfar.apk";

    private final Activity act;
    private final WebView web;
    private volatile boolean busy = false;

    Updater(Activity a, WebView w) { act = a; web = w; }

    int currentCode() {
        try {
            return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionCode;
        } catch (Exception e) { return 0; }
    }

    String currentName() {
        try {
            return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName;
        } catch (Exception e) { return ""; }
    }

    private void js(final String code) {
        act.runOnUiThread(new Runnable() {
            @Override public void run() { if (web != null) web.evaluateJavascript(code, null); }
        });
    }

    /** Result goes to window.__updateInfo({available,name,notes,error}) */
    void check() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(15000);
                    c.setRequestProperty("Accept", "application/vnd.github+json");
                    c.setRequestProperty("User-Agent", "Istighfar-App");
                    if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
                    String body = read(c.getInputStream());
                    JSONObject o = new JSONObject(body);
                    String tag = o.optString("tag_name", "");          // build-N
                    int n = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
                    boolean newer = 100 + n > currentCode();
                    JSONObject r = new JSONObject();
                    r.put("available", newer);
                    r.put("name", o.optString("name", tag));
                    r.put("notes", o.optString("body", ""));
                    r.put("current", currentName());
                    js("window.__updateInfo&&window.__updateInfo(" + r.toString() + ")");
                } catch (Exception e) {
                    js("window.__updateInfo&&window.__updateInfo({error:" + JSONObject.quote(String.valueOf(e.getMessage())) + "})");
                }
            }
        }).start();
    }

    /** Progress -> window.__updateProgress(pct) ; failure -> window.__updateFail(reason) */
    void download() {
        if (busy) return;
        if (Build.VERSION.SDK_INT >= 26 && !act.getPackageManager().canRequestPackageInstalls()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + act.getPackageName()));
            act.startActivity(i);
            js("window.__updateFail&&window.__updateFail('perm')");
            return;
        }
        busy = true;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File dir = new File(act.getCacheDir(), "updates");
                    dir.mkdirs();
                    File out = UpdateProvider.apk(act);
                    File part = new File(dir, "update.part");
                    HttpURLConnection c = (HttpURLConnection) new URL(APK_URL).openConnection();
                    c.setInstanceFollowRedirects(true);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setRequestProperty("User-Agent", "Istighfar-App");
                    if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
                    long total = c.getContentLengthLong();
                    InputStream in = c.getInputStream();
                    FileOutputStream fo = new FileOutputStream(part);
                    byte[] buf = new byte[65536];
                    long got = 0;
                    int last = -1, n;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        got += n;
                        if (total > 0) {
                            int p = (int) (got * 100 / total);
                            if (p != last) { last = p; js("window.__updateProgress&&window.__updateProgress(" + p + ")"); }
                        }
                    }
                    fo.close();
                    in.close();
                    if (got < 100000) throw new Exception("small");
                    if (out.exists()) out.delete();
                    if (!part.renameTo(out)) throw new Exception("rename");
                    install();
                } catch (Exception e) {
                    js("window.__updateFail&&window.__updateFail(" + JSONObject.quote(String.valueOf(e.getMessage())) + ")");
                } finally {
                    busy = false;
                }
            }
        }).start();
    }

    private void install() {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    Uri u = Uri.parse("content://" + act.getPackageName() + ".updates/update.apk");
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(u, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(i);
                    js("window.__updateProgress&&window.__updateProgress(101)");
                } catch (Exception e) {
                    js("window.__updateFail&&window.__updateFail('install')");
                }
            }
        });
    }

    private static String read(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        return b.toString("UTF-8");
    }
}
