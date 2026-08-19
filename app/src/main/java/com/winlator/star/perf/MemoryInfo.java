package com.winlator.star.perf;

import android.content.Context;
import android.app.ActivityManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public final class MemoryInfo {

    private MemoryInfo() {
        // Classe utilitária
    }

    // ============================================================
    // RAM
    // ============================================================

    /**
     * Retorna a RAM física total em bytes.
     */
    public static long getTotalRamBytes() {
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader("/proc/meminfo"));

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    reader.close();

                    String[] parts = line.trim().split("\\s+");

                    if (parts.length >= 2) {
                        // /proc/meminfo normalmente informa em KiB
                        return Long.parseLong(parts[1]) * 1024L;
                    }
                }
            }

            reader.close();

        } catch (Exception ignored) {
        }

        return 0L;
    }

    /**
     * Retorna a RAM disponível em bytes.
     */
    public static long getAvailableRamBytes() {
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader("/proc/meminfo"));

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.startsWith("MemAvailable:")) {
                    reader.close();

                    String[] parts = line.trim().split("\\s+");

                    if (parts.length >= 2) {
                        return Long.parseLong(parts[1]) * 1024L;
                    }
                }
            }

            reader.close();

        } catch (Exception ignored) {
        }

        return 0L;
    }

    /**
     * RAM utilizada em bytes.
     */
    public static long getUsedRamBytes() {
        long total = getTotalRamBytes();
        long available = getAvailableRamBytes();

        if (total <= 0) {
            return 0L;
        }

        return Math.max(0L, total - available);
    }

    // ============================================================
    // ZRAM
    // ============================================================

    /**
     * Procura automaticamente por zram0, zram1, etc.
     */
    private static File findZramDevice() {

        File blockDirectory = new File("/sys/block");

        if (!blockDirectory.exists() || !blockDirectory.isDirectory()) {
            return null;
        }

        File[] files = blockDirectory.listFiles();

        if (files == null) {
            return null;
        }

        for (File file : files) {

            if (file.isDirectory()
                    && file.getName().startsWith("zram")) {

                return file;
            }
        }

        return null;
    }

    /**
     * Retorna o tamanho configurado da ZRAM em bytes.
     *
     * Exemplo:
     * 8589934592 bytes = 8 GiB
     */
    public static long getZramSizeBytes() {

        File zram = findZramDevice();

        if (zram == null) {
            return 0L;
        }

        File diskSize = new File(zram, "disksize");

        if (!diskSize.exists()) {
            return 0L;
        }

        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader(diskSize));

            String value = reader.readLine();

            reader.close();

            if (value != null) {
                return Long.parseLong(value.trim());
            }

        } catch (Exception ignored) {
        }

        return 0L;
    }

    /**
     * Retorna a memória atualmente utilizada pela ZRAM.
     */
    public static long getZramUsedBytes() {

        File zram = findZramDevice();

        if (zram == null) {
            return 0L;
        }

        /*
         * mm_stat possui informações internas da ZRAM.
         *
         * Formato comum:
         *
         * orig_data_size
         * compr_data_size
         * mem_used_total
         * ...
         */

        File mmStat = new File(zram, "mm_stat");

        if (mmStat.exists()) {

            try {
                BufferedReader reader =
                        new BufferedReader(new FileReader(mmStat));

                String line = reader.readLine();

                reader.close();

                if (line != null) {

                    String[] values =
                            line.trim().split("\\s+");

                    if (values.length >= 3) {
                        return Long.parseLong(values[2]);
                    }
                }

            } catch (Exception ignored) {
            }
        }

        /*
         * Fallback: tenta usar mem_used_total diretamente.
         */

        File memUsed = new File(zram, "mem_used_total");

        if (memUsed.exists()) {

            try {
                BufferedReader reader =
                        new BufferedReader(new FileReader(memUsed));

                String value = reader.readLine();

                reader.close();

                if (value != null) {
                    return Long.parseLong(value.trim());
                }

            } catch (Exception ignored) {
            }
        }

        return 0L;
    }

    /**
     * Retorna quanto de ZRAM ainda está disponível.
     */
    public static long getZramAvailableBytes() {

        long total = getZramSizeBytes();
        long used = getZramUsedBytes();

        if (total <= 0) {
            return 0L;
        }

        return Math.max(0L, total - used);
    }

    // ============================================================
    // TOTAL RAM + ZRAM
    // ============================================================

    /**
     * RAM física + tamanho configurado da ZRAM.
     *
     * Exemplo:
     *
     * RAM = 4 GiB
     * ZRAM = 8 GiB
     *
     * Resultado = 12 GiB
     */
    public static long getTotalMemoryWithZramBytes() {
        return getTotalRamBytes() + getZramSizeBytes();
    }

    /**
     * RAM disponível + ZRAM disponível.
     */
    public static long getAvailableMemoryWithZramBytes() {

        return getAvailableRamBytes()
                + getZramAvailableBytes();
    }

    /**
     * RAM usada + ZRAM usada.
     */
    public static long getUsedMemoryWithZramBytes() {

        return getUsedRamBytes()
                + getZramUsedBytes();
    }

    // ============================================================
    // CONVERSÃO
    // ============================================================

    public static double bytesToGiB(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    public static double bytesToMiB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    public static String formatBytes(long bytes) {

        if (bytes <= 0) {
            return "0 B";
        }

        double gib = bytesToGiB(bytes);

        if (gib >= 1.0) {
            return String.format(
                    Locale.US,
                    "%.1f GiB",
                    gib
            );
        }

        double mib = bytesToMiB(bytes);

        return String.format(
                Locale.US,
                "%.0f MiB",
                mib
        );
    }

    // ============================================================
    // INFORMAÇÕES COMPLETAS
    // ============================================================

    public static String getRamInfo() {

        long total = getTotalRamBytes();
        long used = getUsedRamBytes();
        long available = getAvailableRamBytes();

        return String.format(
                Locale.US,
                "RAM: %s / %s\nDisponível: %s",
                formatBytes(used),
                formatBytes(total),
                formatBytes(available)
        );
    }

    public static String getZramInfo() {

        long total = getZramSizeBytes();
        long used = getZramUsedBytes();
        long available = getZramAvailableBytes();

        if (total <= 0) {
            return "ZRAM: não detectada";
        }

        return String.format(
                Locale.US,
                "ZRAM: %s / %s\nDisponível: %s",
                formatBytes(used),
                formatBytes(total),
                formatBytes(available)
        );
    }

    public static String getCompleteInfo() {

        long ramTotal = getTotalRamBytes();
        long ramUsed = getUsedRamBytes();

        long zramTotal = getZramSizeBytes();
        long zramUsed = getZramUsedBytes();

        long total = ramTotal + zramTotal;
        long used = ramUsed + zramUsed;

        return String.format(
                Locale.US,
                "MEMÓRIA USADA:\n%s + %s = %s\n\n" +
                "MEMÓRIA TOTAL:\n%s + %s = %s\n\n" +
                "HUD:\n%s / %s",
                formatBytes(ramUsed),
                formatBytes(zramUsed),
                formatBytes(used),

                formatBytes(ramTotal),
                formatBytes(zramTotal),
                formatBytes(total),

                formatBytes(used),
                formatBytes(total)
        );
    }

    // ============================================================
    // ACTIVITYMANAGER
    // ============================================================

    /**
     * Alternativa usando a API do Android.
     */
    public static long getAndroidTotalRamBytes(Context context) {

        if (context == null) {
            return 0L;
        }

        try {

            ActivityManager manager =
                    (ActivityManager)
                            context.getSystemService(
                                    Context.ACTIVITY_SERVICE
                            );

            if (manager == null) {
                return 0L;
            }

            ActivityManager.MemoryInfo info =
                    new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(info);

            return info.totalMem;

        } catch (Exception ignored) {
        }

        return 0L;
    }

    /**
     * Memória disponível usando API Android.
     */
    public static long getAndroidAvailableRamBytes(Context context) {

        if (context == null) {
            return 0L;
        }

        try {

            ActivityManager manager =
                    (ActivityManager)
                            context.getSystemService(
                                    Context.ACTIVITY_SERVICE
                            );

            if (manager == null) {
                return 0L;
            }

            ActivityManager.MemoryInfo info =
                    new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(info);

            return info.availMem;

        } catch (Exception ignored) {
        }

        return 0L;
    }
}
