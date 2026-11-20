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
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
