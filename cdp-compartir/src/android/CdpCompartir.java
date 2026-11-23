package com.copacd.compartir;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;
import android.app.PendingIntent;
import android.content.pm.PackageInstaller;
import android.content.IntentFilter;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class CdpCompartir extends CordovaPlugin {
    private static final String TAG = "CdpCompartir";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        final File filesDir = cordova.getContext().getFilesDir();
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                File vozAnterior = new File(filesDir, "cdp-voz");
                if (!vozAnterior.exists()) return;
                try {
                    borrarVozAnterior(vozAnterior, filesDir);
                    Log.i(TAG, "Se eliminaron los archivos de voz offline anteriores.");
                } catch (IOException e) {
                    Log.w(TAG, "No se pudieron eliminar todos los archivos de voz anteriores.", e);
                }
            }
        });
    }

    private void borrarVozAnterior(File archivo, File filesDir) throws IOException {
        String root = new File(filesDir.getCanonicalFile(), "cdp-voz").getPath();
        String path = archivo.getCanonicalPath();
        if (!path.equals(root) && !path.startsWith(root + File.separator)) {
            throw new IOException("Ruta fuera de la carpeta de voz anterior.");
        }
        if (archivo.isDirectory()) {
            File[] hijos = archivo.listFiles();
            if (hijos == null) throw new IOException("No se pudo leer " + archivo.getAbsolutePath());
            for (File hijo : hijos) borrarVozAnterior(hijo, filesDir);
        }
        if (!archivo.delete()) throw new IOException("No se pudo borrar " + archivo.getAbsolutePath());
    }

    @Override
    public boolean execute(String accion, CordovaArgs args, final CallbackContext cb) throws JSONException {
        if (!"compartir".equals(accion)) {
            if ("abrir".equals(accion)) { abrir(args, cb); return true; }
            if ("anexar".equals(accion)) { anexar(args, cb); return true; }
            if ("bajar".equals(accion)) { bajar(args, cb); return true; }
            if ("http".equals(accion)) { http(args, cb); return true; }
            if ("instalar".equals(accion)) { instalar(args, cb); return true; }
            if ("descargas".equals(accion)) { abrirDescargas(args, cb); return true; }
            if ("notificar".equals(accion)) { notificar(args, cb); return true; }
            cb.error("accion desconocida: " + accion);
            return false;
        }
        String n0 = args.optString(0);
        String c0 = args.optString(1);
        String t0 = args.optString(2);
        final String nombre = (n0 == null || n0.length() == 0) ? "copa.json" : n0;
        final String contenido = (c0 == null) ? "" : c0;
        final String texto = (t0 == null) ? "" : t0;
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Context ctx = cordova.getContext();
                    File dir = new File(ctx.getFilesDir(), "compartir");
                    if (!dir.exists() && !dir.mkdirs()) {
                        cb.error("No se pudo crear la carpeta de ficheros");
                        return;
                    }
                    String limpio = nombre.replaceAll("[^A-Za-z0-9._-]", "_");
                    if (!limpio.toLowerCase().endsWith(".json")) limpio = limpio + ".json";
                    File f = new File(dir, limpio);
                    OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
                    w.write(contenido);
                    w.close();
                    final Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".cdpfiles", f);
                    final String asunto = limpio;
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent i = new Intent(Intent.ACTION_SEND);
                                i.setType("application/json");
                                i.putExtra(Intent.EXTRA_STREAM, uri);
                                if (texto.length() > 0) i.putExtra(Intent.EXTRA_TEXT, texto);
                                i.setClipData(ClipData.newRawUri(asunto, uri));
                                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Intent chooser = Intent.createChooser(i, "Compartir copa CD");
                                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                cordova.getActivity().startActivity(chooser);
                                cb.success(asunto);
                            } catch (Throwable e) {
                                cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                            }
                        }
                    });
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        });
        return true;
    }

    private File archivoCompartible(String nombre) throws IOException {
        Context ctx = cordova.getContext();
        File dir = new File(ctx.getFilesDir(), "compartir");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("No se pudo crear la carpeta de ficheros");
        String limpio = nombre.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(dir, limpio);
    }

    private void abrir(CordovaArgs args, final CallbackContext cb) {
        final String nombre = args.optString(0) == null || args.optString(0).length() == 0 ? "descarga.apk" : args.optString(0);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    File f = archivoCompartible(nombre);
                    new FileOutputStream(f, false).close();
                    cb.success(f.getName());
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        });
    }

    private void anexar(CordovaArgs args, final CallbackContext cb) {
        final String nombre = args.optString(0);
        final String b64 = args.optString(1);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    File f = archivoCompartible(nombre);
                    byte[] datos = Base64.decode(b64, Base64.DEFAULT);
                    FileOutputStream out = new FileOutputStream(f, true);
                    out.write(datos);
                    out.close();
                    cb.success("ok");
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        });
    }

    private void bajar(CordovaArgs args, final CallbackContext cb) {
        final String url = args.optString(0);
        final String nombre = args.optString(1);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                HttpURLConnection cx = null;
                try {
                    if (url == null || url.length() == 0) throw new IOException("Falta la URL de descarga");
                    File f = archivoCompartible(nombre);
                    cx = (HttpURLConnection) new URL(url).openConnection();
                    cx.setConnectTimeout(15000);
                    cx.setReadTimeout(60000);
                    cx.setInstanceFollowRedirects(true);
                    cx.setRequestProperty("User-Agent", "copa-cd-apk");
                    int codigo = cx.getResponseCode();
                    if (codigo < 200 || codigo >= 300) throw new IOException("El servidor respondió HTTP " + codigo);
                    long total = cx.getContentLength();
                    InputStream in = cx.getInputStream();
                    FileOutputStream out = new FileOutputStream(f, false);
                    byte[] buf = new byte[65536];
                    long leidos = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        leidos += n;
                        PluginResult pr = new PluginResult(PluginResult.Status.OK, progresoJson(leidos, total));
                        pr.setKeepCallback(true);
                        cb.sendPluginResult(pr);
                    }
                    out.close();
                    in.close();
                    cb.success(f.getName());
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                } finally {
                    if (cx != null) cx.disconnect();
                }
            }
        });
    }

    private JSONObject progresoJson(long leidos, long total) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("leidos", leidos);
        o.put("total", total);
        o.put("pct", total > 0 ? Math.round(leidos * 100 / total) : 0);
        return o;
    }

    private void http(CordovaArgs args, final CallbackContext cb) {
        final String url = args.optString(0);
        final String metodo = args.optString(1);
        final String cuerpo = args.optString(2);
        final String ct = args.optString(3);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                HttpURLConnection cx = null;
                try {
                    if (url == null || url.length() == 0) throw new IOException("Falta la URL");
                    cx = (HttpURLConnection) new URL(url).openConnection();
                    cx.setConnectTimeout(15000);
                    cx.setReadTimeout(60000);
                    cx.setInstanceFollowRedirects(true);
                    cx.setRequestMethod(metodo == null || metodo.length() == 0 ? "GET" : metodo);
                    cx.setRequestProperty("User-Agent", "copa-cd-apk");
                    if (ct != null && ct.length() > 0) cx.setRequestProperty("Content-Type", ct);
                    if (cuerpo != null && cuerpo.length() > 0 && !"GET".equals(cx.getRequestMethod()) && !"HEAD".equals(cx.getRequestMethod())) {
                        cx.setDoOutput(true);
                        byte[] datos = cuerpo.getBytes("UTF-8");
                        cx.setFixedLengthStreamingMode(datos.length);
                        OutputStream out = cx.getOutputStream();
                        out.write(datos);
                        out.close();
                    }
                    int code = cx.getResponseCode();
                    InputStream in = code >= 400 ? cx.getErrorStream() : cx.getInputStream();
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    if (in != null) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) bs.write(buf, 0, n);
                        in.close();
                    }
                    JSONObject r = new JSONObject();
                    r.put("code", code);
                    r.put("cuerpo", bs.toString("UTF-8"));
                    cb.success(r);
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                } finally {
                    if (cx != null) cx.disconnect();
                }
            }
        });
    }

    private void abrirDescargas(CordovaArgs args, final CallbackContext cb) {
        final String nombre = args.optString(0);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (nombre != null && nombre.length() > 0) {
                    try {
                        File f = archivoCompartible(nombre);
                        if (f != null && f.exists()) copiarADescargas(f);
                    } catch (Throwable t) {
                        Log.w(TAG, "copiarADescargas fallo", t);
                    }
                }
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            Context ctx = cordova.getContext();
                            Intent d = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                            d.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(d);
                            cb.success("descargas");
                        } catch (Exception e1) {
                            try {
                                Context ctx2 = cordova.getContext();
                                Intent d2 = new Intent("android.intent.action.VIEW_DOWNLOADS");
                                d2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ctx2.startActivity(d2);
                                cb.success("descargas-v2");
                            } catch (Exception e2) {
                                cb.error("No se pudo abrir Descargas: " + e2.getMessage());
                            }
                        }
                    }
                });
            }
        });
    }

    private void instalar(CordovaArgs args, final CallbackContext cb) {
        final String nombre = args.optString(0);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context ctx = cordova.getContext();
                    File f = archivoCompartible(nombre);
                    if (!f.exists() || f.length() == 0) throw new IOException("La descarga no está completa");
                    uriDescargas = copiarADescargas(f);
                    Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".cdpfiles", f);
                    if (Build.VERSION.SDK_INT >= 26 && !puedeInstalar(ctx)) {
                        pedirPermisoInstalar(ctx);
                        cb.error("permiso-instalar: Android pide autorizar 'Instalar apps desconocidas' para Copa CD. Se abrieron los Ajustes: activa el permiso, vuelve a la app y toca Instalar otra vez");
                        return;
                    }
                    /* Via moderna PackageInstaller (Android 12-15): el ACTION_VIEW
                       clasico ya no abre el instalador en equipos nuevos (por eso
                       "toco Instalar y no pasa nada"). Si el sistema no responde,
                       hay respaldo automatico con el ACTION_VIEW clasico. */
                    Intent vIP = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    vIP.setDataAndType(uri, "application/vnd.android.package-archive");
                    vIP.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (uiLanzar(ctx, vIP)) { cb.success("instalador-directo"); return; }
                    if (uriDescargas != null) {
                        Intent vD = new Intent(Intent.ACTION_VIEW);
                        vD.setDataAndType(uriDescargas, "application/vnd.android.package-archive");
                        vD.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (uiLanzar(ctx, vD)) { cb.success("instalador-descargas"); return; }
                    }
                    sesionConfirmada = false;
                    ctxSesion = ctx; uriSesion = uri;
                    if (instalarSesionApi(ctx, f, cb)) {
                        esperarInstaladorSesion(ctx, uri, cb);
                        return;
                    }
                    lanzarInstalador(ctx, uri);
                    cb.success("instalador");
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        });
    }

    /* Espera hasta 8 s la respuesta del sistema (STATUS_PENDING_USER_ACTION o
       resultado final) tras el commit de la sesion. Si el receptor no contesta
       (pasa en algunos equipos), lanza el instalador clasico con FileProvider
       para que el usuario nunca se quede colgado en "abriendo instalador...". */
    private void esperarInstaladorSesion(final Context ctx, final Uri uri, final CallbackContext cb) {
        cordova.getThreadPool().execute(new Runnable() { public void run() {
            for (int i = 0; i < 80; i++) {
                if (sesionConfirmada) return;
                if (cbSesion == null) return;
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
            CallbackContext local = cbSesion; cbSesion = null;
            if (local == null || sesionConfirmada) return;
            try {
                lanzarInstalador(ctx, uriDescargas != null ? uriDescargas : uri);
                local.success("instalador-clasico");
            } catch (Throwable e) {
                local.error("instalador: el sistema no respondio y el respaldo clasico tampoco; instala a mano desde Archivos -> Descargas");
            }
        }});
    }

    private boolean puedeInstalar(Context ctx) {
        try { return ctx.getPackageManager().canRequestPackageInstalls(); }
        catch (Throwable e) { return true; }
    }

    private void pedirPermisoInstalar(Context ctx) {
        try {
            Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            s.setData(Uri.parse("package:" + ctx.getPackageName()));
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(s);
        } catch (Throwable e) { Log.w("CdpCompartir", "No se pudo abrir Ajustes de instalacion", e); }
    }

    private String[] pendienteNotif;

    @Override
    public void onRequestPermissionResult(int code, String[] perms, int[] results) throws JSONException {
        if (code == 7788 && pendienteNotif != null) {
            String[] datos = pendienteNotif;
            pendienteNotif = null;
            if (results != null && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) notificarYa(datos[0], datos[1]);
        }
    }

    private void notificar(CordovaArgs args, final CallbackContext cb) {
        try {
            String t0 = args.optString(0);
            String x0 = args.optString(1);
            final String titulo = (t0 == null || t0.length() == 0) ? "Copa CD" : t0;
            final String texto = (x0 == null) ? "" : x0;
            Context ctx = cordova.getContext();
            if (Build.VERSION.SDK_INT >= 33 && !cordova.hasPermission("android.permission.POST_NOTIFICATIONS")) {
                pendienteNotif = new String[]{titulo, texto};
                cordova.requestPermission(this, 7788, "android.permission.POST_NOTIFICATIONS");
                cb.success("pendiente");
                return;
            }
            notificarYa(titulo, texto);
            cb.success("notificado");
        } catch (Throwable e) { cb.error(e.getMessage() == null ? "no se pudo notificar" : e.getMessage()); }
    }

    private void notificarYa(String titulo, String texto) {
        try {
            Context ctx = cordova.getContext();
            android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            String idCanal = "actualizaciones";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel canal = new android.app.NotificationChannel(idCanal, "Actualizaciones de Copa CD", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                canal.setDescription("Avisos de nuevas versiones de la APK");
                nm.createNotificationChannel(canal);
            }
            android.content.Intent abrir = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            if (abrir == null) abrir = new android.content.Intent();
            android.app.PendingIntent pende = android.app.PendingIntent.getActivity(ctx, 2001, abrir, android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(ctx, idCanal) : new android.app.Notification.Builder(ctx);
            nb.setSmallIcon(ctx.getApplicationInfo().icon).setContentTitle(titulo).setContentText(texto)
              .setAutoCancel(true).setContentIntent(pende)
              .setStyle(new android.app.Notification.BigTextStyle().bigText(texto));
            nm.notify(2001, nb.build());
        } catch (Throwable e) { Log.w("CdpCompartir", "fallo notificar", e); }
    }

    private android.content.BroadcastReceiver receptorSesion;
    private CallbackContext cbSesion;
    private volatile boolean sesionConfirmada;
    private Context ctxSesion;
    private Uri uriSesion;
    private Uri uriDescargas;

    /* Instalacion via PackageInstaller.Session: arma la sesion, copia la APK
       dentro y hace commit; el SISTEMA muestra el dialogo "Instalar". Así
       funciona igual en Android 14/15 donde el ACTION_VIEW clasico murio. */
    private boolean instalarSesionApi(final Context ctx, final File f, final CallbackContext cb) {
        PackageInstaller.Session ses = null;
        try {
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            PackageInstaller pkg = ctx.getPackageManager().getPackageInstaller();
            int idS = pkg.createSession(params);
            ses = pkg.openSession(idS);
            java.io.InputStream in = new java.io.FileInputStream(f);
            OutputStream os = ses.openWrite("copacd", 0, f.length());
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            ses.fsync(os);
            os.close();
            in.close();
            cbSesion = cb;
            Intent aviso = new Intent("COPACD_INSTALADOR_ESTADO").setPackage(ctx.getPackageName());
            int bandera = android.os.Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent pende = PendingIntent.getBroadcast(ctx, 3002, aviso, bandera | PendingIntent.FLAG_UPDATE_CURRENT);
            if (receptorSesion != null) { try { ctx.unregisterReceiver(receptorSesion); } catch (Throwable t) { } }
            receptorSesion = new android.content.BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    try {
                        android.os.Bundle ext = i.getExtras();
                        if (ext == null || !ext.containsKey(PackageInstaller.EXTRA_STATUS)) return;
                        int estado = ext.getInt(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
                        if (estado == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                            Intent act = null;
                            try { act = (Intent) ext.getParcelable(Intent.EXTRA_INTENT); } catch (Throwable te) { act = null; }
                            if (act != null) {
                                act.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                if (uiLanzar(c, act)) { sesionConfirmada = true; return; }
                            }
                            /* La confirmacion del sistema no se pudo abrir: respaldo clasico al toca */
                            CallbackContext local2 = cbSesion; cbSesion = null;
                            try { c.unregisterReceiver(this); } catch (Throwable t) { }
                            if (local2 == null) return;
                            try { lanzarInstalador(ctxSesion, uriDescargas != null ? uriDescargas : uriSesion); local2.success("instalador-clasico"); }
                            catch (Throwable e2) { local2.error("instalador: " + (e2.getMessage() == null ? e2.getClass().getSimpleName() : e2.getMessage())); }
                            return;
                        }
                        CallbackContext local = cbSesion; cbSesion = null;
                        try { c.unregisterReceiver(this); } catch (Throwable t) { }
                        if (local == null) return;
                        if (estado == PackageInstaller.STATUS_SUCCESS) local.success("instalador-sistema");
                        else local.error("instalador: " + (ext.getString(PackageInstaller.EXTRA_STATUS_MESSAGE) == null ? ("estado " + estado) : ext.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)));
                    } catch (Throwable e) {
                        CallbackContext local = cbSesion; cbSesion = null;
                        if (local != null) local.error("instalador: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                    }
                }
            };
            IntentFilter filtro = new IntentFilter("COPACD_INSTALADOR_ESTADO");
            /* El remitente del estado de instalacion es el SISTEMA (PackageInstaller),
               por eso el receptor debe ser EXPORTED: con NOT_EXPORTED Android 13/14
               lo bloqueaba y el dialogo de instalacion nunca se abria. */
            if (android.os.Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receptorSesion, filtro, Context.RECEIVER_EXPORTED);
            else ctx.registerReceiver(receptorSesion, filtro);
            ses.commit(pende.getIntentSender());
            ses.close();
            return true;
        } catch (Throwable e) {
            if (ses != null) { try { ses.abandon(); } catch (Throwable t) { } }
            Log.w("CdpCompartir", "sesion de instalacion no disponible: " + e);
            return false;
        }
    }

    /* Lanza la actividad en el hilo de UI (obligatorio en varios telefonos) y
       avisa con el boolean si realmente se lanzo; bloquea hasta 1 segundo. */
    private boolean uiLanzar(final Context ctx, final Intent intent) {
        final java.util.concurrent.atomic.AtomicBoolean lanzado = new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try { ctx.startActivity(intent); lanzado.set(true); }
                    catch (Throwable e) { Log.w("CdpCompartir", "fallo lanzar instalador", e); }
                }
            });
        } catch (Throwable e) { return false; }
        for (int j = 0; j < 20 && !lanzado.get(); j++) {
            try { Thread.sleep(50); } catch (InterruptedException e) { break; }
        }
        return lanzado.get();
    }

    private void lanzarInstalador(final Context ctx, final Uri uri) throws IOException {
        Intent base = new Intent(Intent.ACTION_VIEW);
        base.setDataAndType(uri, "application/vnd.android.package-archive");
        base.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (uiLanzar(ctx, base)) return;
        Intent elegir = Intent.createChooser(base, "Instalar actualización de Copa CD");
        elegir.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (uiLanzar(ctx, elegir)) return;
        String[] instaladores = {"com.google.android.packageinstaller", "com.android.packageinstaller"};
        for (String paq : instaladores) {
            try {
                Intent i2 = new Intent(Intent.ACTION_VIEW);
                i2.setPackage(paq);
                i2.setDataAndType(uri, "application/vnd.android.package-archive");
                i2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                if (uiLanzar(ctx, i2)) return;
            } catch (Throwable e3) { }
        }
        throw new IOException("Ningún instalador de Android respondió; instala la APK desde la carpeta Descargas del móvil");
    }

    private Uri copiarADescargas(File f) {
        try {
            Context ctx = cordova.getContext();
            if (android.os.Build.VERSION.SDK_INT < 29) return null;
            /* Si ya existe una copia previa del mismo nombre, se reutiliza (la
               carga limpia el contenido), siguiendo el mismo camino que el
               archivo descargado a mano: ContentResolver + Descargas. */
            /* Limpia copias anteriores de Copa CD en Descargas (propias de esta
               app): asi solo queda el APK de la version de ahora y no se puede
               instalar por error una APK vieja que siga en la carpeta. */
            try {
                android.database.Cursor cur = ctx.getContentResolver().query(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{android.provider.MediaStore.Downloads._ID},
                    android.provider.MediaStore.Downloads.DISPLAY_NAME + " LIKE ?",
                    new String[]{"CopaCD%"}, null);
                if (cur != null) {
                    while (cur.moveToNext()) {
                        long idf = cur.getLong(0);
                        try {
                            ctx.getContentResolver().delete(
                                android.content.ContentUris.withAppendedId(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, idf),
                                null, null);
                        } catch (Throwable t3) { }
                    }
                    cur.close();
                }
            } catch (Throwable t2) { }
            android.content.ContentValues v = new android.content.ContentValues();
            v.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, f.getName());
            v.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            android.net.Uri destino = ctx.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (destino == null) return null;
            java.io.InputStream in = new java.io.FileInputStream(f);
            java.io.OutputStream os = ctx.getContentResolver().openOutputStream(destino);
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) os.write(b, 0, n);
            os.close();
            in.close();
            return destino;
        } catch (Throwable t) { }
        return null;
    }
}
