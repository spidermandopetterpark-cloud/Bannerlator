package com.winlator.star.perf;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;
import rikka.shizuku.Shizuku.UserServiceArgs;

public final class ZramManager {

    private static final String ZRAM =
            "/sys/block/zram0";

    private static final String ZRAM_DEV =
            "/dev/block/zram0";

    private static final long EIGHT_GIB =
            8L * 1024L * 1024L * 1024L;

    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    private static ZramUserService service;

    private static boolean connected = false;

    private static final ServiceConnection CONNECTION =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(
                        ComponentName name,
                        IBinder binder) {

                    service =
                            ZramUserService.Stub.asInterface(
                                    binder
                            );

                    connected = true;
                }

                @Override
                public void onServiceDisconnected(
                        ComponentName name) {

                    connected = false;
                    service = null;
                }
            };

    private ZramManager() {
    }

    // ============================================================
    // SHIZUKU
    // ============================================================

    public static boolean isShizukuAvailable() {

        try {
            return Shizuku.pingBinder();
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean hasShizukuPermission() {

        if (!isShizukuAvailable()) {
            return false;
        }

        try {

            return Shizuku.checkSelfPermission()
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;

        } catch (Throwable e) {
            return false;
        }
    }

    public static void requestPermission() {

        if (!isShizukuAvailable()) {
            return;
        }

        if (hasShizukuPermission()) {
            return;
        }

        try {

            Shizuku.requestPermission(
                    SHIZUKU_PERMISSION_REQUEST_CODE
            );

        } catch (Throwable ignored) {
        }
    }

    // ============================================================
    // USER SERVICE
    // ============================================================

    public static boolean connect() {

        if (!isShizukuAvailable()) {
            return false;
        }

        if (!hasShizukuPermission()) {
            requestPermission();
            return false;
        }

        if (connected && service != null) {
            return true;
        }

        try {

            UserServiceArgs args =
                    new UserServiceArgs(
                            new ComponentName(
                                    "com.winlator.star",
                                    ZramUserService.class.getName()
                            )
                    )
                    .daemon(false)
                    .version(1)
                    .tag("BannerlatorZram");

            Shizuku.bindUserService(
                    args,
                    CONNECTION
            );

            return true;

        } catch (Throwable e) {

            return false;
        }
    }

    // ============================================================
    // EXECUTAR COMANDO
    // ============================================================

    private static CommandResult execute(
            String command
    ) {

        if (!connect()) {

            return new CommandResult(
                    false,
                    "",
                    "Shizuku não disponível"
            );
        }

        if (service == null) {

            return new CommandResult(
                    false,
                    "",
                    "ZramUserService não conectado"
            );
        }

        try {

            return service.execute(command);

        } catch (Throwable e) {

            return new CommandResult(
                    false,
                    "",
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // DETECTAR ZRAM
    // ============================================================

    public static boolean exists() {

        return new File(ZRAM).exists();
    }

    public static long getConfiguredSizeBytes() {

        File file =
                new File(
                        ZRAM + "/disksize"
                );

        if (!file.exists()) {
            return 0L;
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file)
                    );

            String value =
                    reader.readLine();

            reader.close();

            if (value == null) {
                return 0L;
            }

            return Long.parseLong(
                    value.trim()
            );

        } catch (Exception e) {

            return 0L;
        }
    }

    public static boolean is8GiB() {

        return getConfiguredSizeBytes()
                == EIGHT_GIB;
    }

    // ============================================================
    // ALGORITMO
    // ============================================================

    public static String getCompressionAlgorithms() {

        File file =
                new File(
                        ZRAM
                                + "/comp_algorithm"
                );

        if (!file.exists()) {
            return "";
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file)
                    );

            String result =
                    reader.readLine();

            reader.close();

            return result == null
                    ? ""
                    : result;

        } catch (Exception e) {

            return "";
        }
    }

    private static String chooseAlgorithm() {

        String algorithms =
                getCompressionAlgorithms();

        if (algorithms.contains("zstd")) {
            return "zstd";
        }

        if (algorithms.contains("lz4")) {
            return "lz4";
        }

        if (algorithms.contains("lzo")) {
            return "lzo";
        }

        return "";
    }

    // ============================================================
    // CONFIGURAR 8 GiB
    // ============================================================

    public static boolean configure8GiB() {

        if (!isShizukuAvailable()) {
            return false;
        }

        if (!hasShizukuPermission()) {
            requestPermission();
            return false;
        }

        if (!exists()) {
            return false;
        }

        /*
         * Desativa o swap atual.
         */
        CommandResult swapOff =
                execute(
                        "swapoff "
                                + ZRAM_DEV
                );

        /*
         * Reset da ZRAM.
         */
        CommandResult reset =
                execute(
                        "sh -c 'echo 1 > "
                                + ZRAM
                                + "/reset'"
                );

        /*
         * Escolhe algoritmo.
         */
        String algorithm =
                chooseAlgorithm();

        if (!algorithm.isEmpty()) {

            execute(
                    "sh -c 'echo "
                            + algorithm
                            + " > "
                            + ZRAM
                            + "/comp_algorithm'"
            );
        }

        /*
         * 8 GiB = 8589934592 bytes
         */
        CommandResult size =
                execute(
                        "sh -c 'echo "
                                + EIGHT_GIB
                                + " > "
                                + ZRAM
                                + "/disksize'"
                );

        if (!size.success) {

            return false;
        }

        /*
         * Formata como swap.
         */
        CommandResult makeSwap =
                execute(
                        "mkswap "
                                + ZRAM_DEV
                );

        if (!makeSwap.success) {

            return false;
        }

        /*
         * Ativa.
         */
        CommandResult swapOn =
                execute(
                        "swapon "
                                + ZRAM_DEV
                );

        if (!swapOn.success) {

            return false;
        }

        /*
         * Confirma no sysfs.
         */
        return is8GiB();
    }

    // ============================================================
    // STATUS
    // ============================================================

    public static String getStatus() {

        if (!isShizukuAvailable()) {

            return "Shizuku: OFF";
        }

        if (!hasShizukuPermission()) {

            return "Shizuku: sem permissão";
        }

        if (!exists()) {

            return "ZRAM: não encontrada";
        }

        long size =
                getConfiguredSizeBytes();

        if (size <= 0) {

            return "ZRAM: desativada";
        }

        return "ZRAM: "
                + MemoryInfo.formatBytes(size);
    }

    public static String getDetailedStatus() {

        StringBuilder result =
                new StringBuilder();

        result.append("BANNERLATOR ZRAM\n");
        result.append("====================\n");

        result.append("Shizuku: ")
                .append(
                        isShizukuAvailable()
                                ? "OK"
                                : "OFF"
                )
                .append("\n");

        result.append("Permissão: ")
                .append(
                        hasShizukuPermission()
                                ? "OK"
                                : "NEGADA"
                )
                .append("\n");

        result.append("zram0: ")
                .append(
                        exists()
                                ? "OK"
                                : "NÃO ENCONTRADA"
                )
                .append("\n");

        long size =
                getConfiguredSizeBytes();

        result.append("Tamanho: ")
                .append(
                        MemoryInfo.formatBytes(size)
                )
                .append("\n");

        result.append("Alvo: 8.0 GiB\n");

        result.append("Configuração: ")
                .append(
                        is8GiB()
                                ? "8 GiB ATIVOS"
                                : "NÃO CONFIGURADA"
                )
                .append("\n");

        result.append("Algoritmos: ")
                .append(
                        getCompressionAlgorithms()
                );

        return result.toString();
    }

    // ============================================================
    // RESULTADO
    // ============================================================

    public static final class CommandResult {

        public final boolean success;
        public final String output;
        public final String error;

        public CommandResult(
                boolean success,
                String output,
                String error
        ) {

            this.success = success;
            this.output = output;
            this.error = error;
        }
    }

    // ============================================================
    // USER SERVICE
    // ============================================================

    public static class ZramUserService
            extends ZramUserServiceStub {

        public ZramUserService() {
            super();
        }
    }
    }
