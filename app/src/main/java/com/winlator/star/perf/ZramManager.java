package com.winlator.star.perf;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

import rikka.shizuku.Shizuku;

/**
 * ZramManager
 *
 * Gerenciamento de ZRAM através do Shizuku.
 *
 * Não depende de:
 * - ZramUserServiceStub
 * - UserService customizado
 * - libsu para operações Shizuku
 *
 * O Shizuku precisa estar ativo e autorizado.
 */
public final class ZramManager {

    private static final String TAG = "ZramManager";

    private static final String ZRAM_BASE =
            "/sys/block/zram0";

    private static final String ZRAM_DISKSIZE =
            ZRAM_BASE + "/disksize";

    private static final String ZRAM_RESET =
            ZRAM_BASE + "/reset";

    private static final String ZRAM_COMP_ALGO =
            ZRAM_BASE + "/comp_algorithm";

    private static final String ZRAM_MAX_COMP_STREAMS =
            ZRAM_BASE + "/max_comp_streams";

    private static final String ZRAM_MEM_LIMIT =
            ZRAM_BASE + "/mem_limit";

    private static final String PROC_SWAPS =
            "/proc/swaps";

    private static final String DEV_BLOCK_ZRAM =
            "/dev/block/zram0";

    private static final String DEV_ZRAM =
            "/dev/zram0";

    private final Context context;

    public ZramManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "context == null"
            );
        }

        this.context =
                context.getApplicationContext();
    }

    // ============================================================
    // SHIZUKU
    // ============================================================

    public boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean hasShizukuPermission() {
        try {
            if (!isShizukuAvailable()) {
                return false;
            }

            if (Build.VERSION.SDK_INT < 23) {
                return false;
            }

            return Shizuku.checkSelfPermission()
                    == PackageManager.PERMISSION_GRANTED;

        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean canUseShizuku() {
        return isShizukuAvailable()
                && hasShizukuPermission();
    }

    public void requestShizukuPermission(
            int requestCode
    ) {
        try {
            if (!isShizukuAvailable()) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 23
                    && Shizuku.checkSelfPermission()
                    != PackageManager.PERMISSION_GRANTED) {

                Shizuku.requestPermission(
                        requestCode
                );
            }

        } catch (Throwable ignored) {
        }
    }

    // ============================================================
    // ZRAM STATUS
    // ============================================================

    public boolean isZramAvailable() {
        try {
            if (new File(ZRAM_BASE).exists()) {
                return true;
            }

            String result =
                    execute(
                            "test -d '" +
                            ZRAM_BASE +
                            "'"
                    );

            return lastExitCode == 0
                    && result != null;

        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isZramSwapEnabled() {

        CommandResult result =
                executeCommand(
                        "cat " + PROC_SWAPS
                );

        if (!result.success) {
            return false;
        }

        String output =
                result.output.toLowerCase(
                        Locale.US
                );

        return output.contains(
                    "/dev/block/zram0"
                )
                || output.contains(
                    "/dev/zram0"
                )
                || output.contains(
                    "zram0"
                );
    }

    public long getZramSize() {

        String value =
                readFile(
                        ZRAM_DISKSIZE
                );

        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(
                    value.trim()
            );

        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public long getZramSizeMiB() {

        long bytes =
                getZramSize();

        if (bytes <= 0) {
            return 0L;
        }

        return bytes
                / 1024L
                / 1024L;
    }

    public String getCompressionAlgorithm() {

        String value =
                readFile(
                        ZRAM_COMP_ALGO
                );

        if (value == null
                || value.trim().isEmpty()) {
            return "";
        }

        String[] algorithms =
                value.trim().split(
                        "\\s+"
                );

        for (String algorithm :
                algorithms) {

            if (algorithm.startsWith("[")
                    && algorithm.endsWith("]")) {

                return algorithm.substring(
                        1,
                        algorithm.length() - 1
                );
            }
        }

        return value.trim();
    }

    public String getCompressionAlgorithms() {
        return readFile(
                ZRAM_COMP_ALGO
        );
    }

    // ============================================================
    // ZRAM SIZE
    // ============================================================

    public boolean setZramSizeMiB(
            long mib
    ) {

        if (mib <= 0) {
            return false;
        }

        if (mib > Long.MAX_VALUE / 1024L / 1024L) {
            return false;
        }

        long bytes =
                mib * 1024L * 1024L;

        return setZramSizeBytes(
                bytes
        );
    }

    public boolean setZramSizeBytes(
            long bytes
    ) {

        if (bytes <= 0) {
            return false;
        }

        if (!canUseShizuku()) {
            return false;
        }

        if (!isZramAvailable()) {
            return false;
        }

        if (isZramSwapEnabled()) {

            if (!disableSwap()) {
                return false;
            }
        }

        CommandResult reset =
                executeCommand(
                        "echo 1 > " +
                        ZRAM_RESET
                );

        if (!reset.success) {
            return false;
        }

        CommandResult size =
                executeCommand(
                        "echo " +
                        bytes +
                        " > " +
                        ZRAM_DISKSIZE
                );

        if (!size.success) {
            return false;
        }

        return getZramSize() == bytes;
    }

    // ============================================================
    // COMPRESSION
    // ============================================================

    public boolean setCompressionAlgorithm(
            String algorithm
    ) {

        if (algorithm == null) {
            return false;
        }

        String safeAlgorithm =
                sanitizeValue(
                        algorithm
                );

        if (safeAlgorithm.isEmpty()) {
            return false;
        }

        if (!canUseShizuku()) {
            return false;
        }

        if (!isZramAvailable()) {
            return false;
        }

        CommandResult result =
                executeCommand(
                        "echo '" +
                        safeAlgorithm +
                        "' > " +
                        ZRAM_COMP_ALGO
                );

        if (!result.success) {
            return false;
        }

        String current =
                getCompressionAlgorithm();

        return safeAlgorithm.equalsIgnoreCase(
                current
        );
    }

    public boolean setMaxCompressionStreams(
            int streams
    ) {

        if (streams <= 0) {
            return false;
        }

        if (!canUseShizuku()) {
            return false;
        }

        if (!isZramAvailable()) {
            return false;
        }

        CommandResult result =
                executeCommand(
                        "echo " +
                        streams +
                        " > " +
                        ZRAM_MAX_COMP_STREAMS
                );

        return result.success;
    }

    public boolean setMemoryLimitMiB(
            long mib
    ) {

        if (mib <= 0) {
            return false;
        }

        if (mib >
                Long.MAX_VALUE / 1024L / 1024L) {
            return false;
        }

        if (!canUseShizuku()) {
            return false;
        }

        long bytes =
                mib * 1024L * 1024L;

        CommandResult result =
                executeCommand(
                        "echo " +
                        bytes +
                        " > " +
                        ZRAM_MEM_LIMIT
                );

        return result.success;
    }

    // ============================================================
    // SWAP
    // ============================================================

    public boolean enableSwap() {

        if (!canUseShizuku()) {
            return false;
        }

        if (!isZramAvailable()) {
            return false;
        }

        if (getZramSize() <= 0) {
            return false;
        }

        if (isZramSwapEnabled()) {
            return true;
        }

        String command =
                "if [ -e " +
                DEV_BLOCK_ZRAM +
                " ]; then " +
                "DEV=" +
                DEV_BLOCK_ZRAM +
                "; " +
                "elif [ -e " +
                DEV_ZRAM +
                " ]; then " +
                "DEV=" +
                DEV_ZRAM +
                "; " +
                "else exit 1; fi; " +
                "mkswap \"$DEV\" >/dev/null 2>&1 || exit 1; " +
                "swapon \"$DEV\"";

        CommandResult result =
                executeCommand(
                        command
                );

        SystemClock.sleep(150);

        return result.success
                && isZramSwapEnabled();
    }

    public boolean disableSwap() {

        if (!canUseShizuku()) {
            return false;
        }

        String command =
                "if [ -e " +
                DEV_BLOCK_ZRAM +
                " ]; then " +
                "swapoff " +
                DEV_BLOCK_ZRAM +
                " 2>/dev/null; " +
                "fi; " +
                "if [ -e " +
                DEV_ZRAM +
                " ]; then " +
                "swapoff " +
                DEV_ZRAM +
                " 2>/dev/null; " +
                "fi";

        executeCommand(
                command
        );

        SystemClock.sleep(150);

        return !isZramSwapEnabled();
    }

    public boolean reset() {

        if (!canUseShizuku()) {
            return false;
        }

        disableSwap();

        if (!isZramAvailable()) {
            return false;
        }

        CommandResult result =
                executeCommand(
                        "echo 1 > " +
                        ZRAM_RESET
                );

        if (!result.success) {
            return false;
        }

        SystemClock.sleep(150);

        return getZramSize() == 0;
    }

    // ============================================================
    // CONFIGURAÇÃO COMPLETA
    // ============================================================

    public boolean configure(
            long sizeMiB,
            String algorithm
    ) {

        if (!canUseShizuku()) {
            return false;
        }

        if (!isZramAvailable()) {
            return false;
        }

        if (!disableSwap()) {
            /*
             * Se não estava ativo, disableSwap()
             * pode retornar true.
             */
            if (isZramSwapEnabled()) {
                return false;
            }
        }

        if (!setZramSizeMiB(sizeMiB)) {
            return false;
        }

        if (algorithm != null
                && !algorithm.trim().isEmpty()) {

            if (!setCompressionAlgorithm(
                    algorithm
            )) {
                return false;
            }
        }

        return enableSwap();
    }

    // ============================================================
    // EXECUÇÃO SHIZUKU
    // ============================================================

    private volatile int lastExitCode =
            -1;

    /**
     * Compatibilidade com chamadas existentes.
     *
     * Retorna apenas stdout.
     */
    public String execute(
            String command
    ) {

        CommandResult result =
                executeCommand(
                        command
                );

        lastExitCode =
                result.exitCode;

        return result.output;
    }

    private CommandResult executeCommand(
            String command
    ) {

        if (command == null
                || command.trim().isEmpty()) {

            return new CommandResult(
                    false,
                    -1,
                    ""
            );
        }

        if (!canUseShizuku()) {

            return new CommandResult(
                    false,
                    -1,
                    ""
            );
        }

        Process process =
                null;

        BufferedReader reader =
                null;

        try {

            String[] args = {
                    "sh",
                    "-c",
                    command
            };

            process =
                    Shizuku.newProcess(
                            args,
                            null,
                            null
                    );

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );

            StringBuilder output =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                output
                        .append(line)
                        .append('\n');
            }

            int exitCode =
                    process.waitFor();

            lastExitCode =
                    exitCode;

            String text =
                    output
                            .toString()
                            .trim();

            return new CommandResult(
                    exitCode == 0,
                    exitCode,
                    text
            );

        } catch (Throwable ignored) {

            lastExitCode = -1;

            return new CommandResult(
                    false,
                    -1,
                    ""
            );

        } finally {

            if (reader != null) {

                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }

            if (process != null) {

                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    // ============================================================
    // FILE READ
    // ============================================================

    public String readFile(
            String path
    ) {

        if (path == null
                || path.trim().isEmpty()) {
            return null;
        }

        String safePath =
                sanitizePath(
                        path
                );

        if (safePath.isEmpty()) {
            return null;
        }

        CommandResult result =
                executeCommand(
                        "cat '" +
                        safePath +
                        "' 2>/dev/null"
                );

        if (!result.success
                || result.output.isEmpty()) {

            return null;
        }

        return result.output.trim();
    }

    // ============================================================
    // UTILITÁRIOS
    // ============================================================

    private String sanitizeValue(
            String value
    ) {

        return value
                .trim()
                .replace("'", "")
                .replace("\"", "")
                .replace(";", "")
                .replace("&", "")
                .replace("|", "")
                .replace("$", "")
                .replace("`", "")
                .replace("\n", "")
                .replace("\r", "");
    }

    private String sanitizePath(
            String path
    ) {

        return path
                .trim()
                .replace("'", "")
                .replace("\"", "")
                .replace(";", "")
                .replace("&", "")
                .replace("|", "")
                .replace("$", "")
                .replace("`", "")
                .replace("\n", "")
                .replace("\r", "");
    }

    private static final class CommandResult {

        final boolean success;

        final int exitCode;

        final String output;

        CommandResult(
                boolean success,
                int exitCode,
                String output
        ) {

            this.success =
                    success;

            this.exitCode =
                    exitCode;

            this.output =
                    output == null
                            ? ""
                            : output;
        }
    }

    // ============================================================
    // STATUS
    // ============================================================

    public String getStatus() {

        StringBuilder status =
                new StringBuilder();

        status.append(
                "ZRAM\n"
        );

        status.append(
                "====================\n"
        );

        status.append(
                "Shizuku: "
        ).append(
                isShizukuAvailable()
                        ? "OK"
                        : "OFF"
        ).append('\n');

        status.append(
                "Permissão: "
        ).append(
                hasShizukuPermission()
                        ? "OK"
                        : "NEGADA"
        ).append('\n');

        status.append(
                "ZRAM: "
        ).append(
                isZramAvailable()
                        ? "OK"
                        : "NÃO ENCONTRADA"
        ).append('\n');

        status.append(
                "Tamanho: "
        ).append(
                getZramSizeMiB()
        ).append(
                " MiB\n"
        );

        status.append(
                "Algoritmo: "
        ).append(
                getCompressionAlgorithm()
        ).append('\n');

        status.append(
                "Swap: "
        ).append(
                isZramSwapEnabled()
                        ? "ATIVO"
                        : "INATIVO"
        ).append('\n');

        return status.toString();
    }

    // ============================================================
    // CONTEXT
    // ============================================================

    public Context getContext() {
        return context;
    }
}
