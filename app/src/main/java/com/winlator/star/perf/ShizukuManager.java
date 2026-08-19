package com.winlator.star.perf;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import rikka.shizuku.Shizuku;

/**
 * ShizukuManager
 *
 * Inicialização e controle da autorização Shizuku
 * utilizada pelo Bannerlator.
 *
 * Funciona com os três flavors:
 *
 * com.winlator.banner
 * com.ludashi.benchmark
 * com.tencent.ig
 *
 * O applicationId é tratado pelo Manifest através
 * de ${applicationId}.
 */
public final class ShizukuManager {

    public static final int REQUEST_CODE =
            ZramManager.SHIZUKU_PERMISSION_REQUEST_CODE;

    private final Context context;

    private final Shizuku.OnRequestPermissionResultListener
            permissionListener;

    public interface Listener {

        void onShizukuAvailable();

        void onShizukuUnavailable();

        void onPermissionGranted();

        void onPermissionDenied();
    }

    private Listener listener;

    public ShizukuManager(
            Context context
    ) {

        this.context =
                context.getApplicationContext();

        permissionListener =
                (requestCode, result) -> {

                    if (requestCode != REQUEST_CODE) {
                        return;
                    }

                    if (result ==
                            PackageManager.PERMISSION_GRANTED) {

                        if (listener != null) {
                            listener.onPermissionGranted();
                        }

                    } else {

                        if (listener != null) {
                            listener.onPermissionDenied();
                        }
                    }
                };

        Shizuku.addRequestPermissionResultListener(
                permissionListener
        );
    }

    // =========================================================
    // LISTENER
    // =========================================================

    public void setListener(
            Listener listener
    ) {

        this.listener =
                listener;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public boolean isAvailable() {

        try {

            return Shizuku.pingBinder();

        } catch (Throwable e) {

            return false;
        }
    }

    public boolean hasPermission() {

        try {

            if (!isAvailable()) {
                return false;
            }

            return Shizuku.checkSelfPermission()
                    == PackageManager.PERMISSION_GRANTED;

        } catch (Throwable e) {

            return false;
        }
    }

    public boolean isReady() {

        return isAvailable()
                && hasPermission();
    }

    // =========================================================
    // REQUEST
    // =========================================================

    public void requestPermission() {

        try {

            if (!isAvailable()) {

                if (listener != null) {
                    listener.onShizukuUnavailable();
                }

                return;
            }

            if (hasPermission()) {

                if (listener != null) {
                    listener.onPermissionGranted();
                }

                return;
            }

            Shizuku.requestPermission(
                    REQUEST_CODE
            );

        } catch (Throwable e) {

            if (listener != null) {
                listener.onPermissionDenied();
            }
        }
    }

    // =========================================================
    // REQUEST COM ACTIVITY
    // =========================================================

    public void requestPermission(
            Activity activity
    ) {

        if (activity == null) {
            requestPermission();
            return;
        }

        requestPermission();
    }

    // =========================================================
    // RATIONALE
    // =========================================================

    public boolean shouldShowPermissionRationale() {

        try {

            if (!isAvailable()) {
                return false;
            }

            if (hasPermission()) {
                return false;
            }

            return Shizuku
                    .shouldShowRequestPermissionRationale();

        } catch (Throwable e) {

            return false;
        }
    }

    // =========================================================
    // STATUS TEXT
    // =========================================================

    public String getStatus() {

        if (!isAvailable()) {

            return "Shizuku não está ativo";
        }

        if (!hasPermission()) {

            return "Shizuku ativo, permissão necessária";
        }

        return "Shizuku autorizado";
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    public void destroy() {

        try {

            Shizuku.removeRequestPermissionResultListener(
                    permissionListener
            );

        } catch (Throwable ignored) {
        }

        listener = null;
    }

    public Context getContext() {

        return context;
    }
}
