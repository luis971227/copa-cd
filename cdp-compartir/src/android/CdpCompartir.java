package com.copacd.compartir;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

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

    private void instalar(CordovaArgs args, final CallbackContext cb) {
        final String nombre = args.optString(0);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context ctx = cordova.getContext();
                    File f = archivoCompartible(nombre);
                    if (!f.exists()) throw new IOException("La descarga no está completa");
                    Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".cdpfiles", f);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    cb.success("instalador");
                } catch (Throwable e) {
                    cb.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                }
            }
        });
    }
}
