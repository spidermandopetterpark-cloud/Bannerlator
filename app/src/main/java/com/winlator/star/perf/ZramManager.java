package com.winlator.star.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class ZramManager {

    private static final String ZRAM_DEVICE = "/dev/block/zram0";
    private static final String ZRAM_SYS = "/sys/block/zram0";

    // 8 GiB
    private static final long ZRAM_SIZE_BYTES =
            8L * 1024L * 1024L * 1024L;

    private ZramManager() {
    }

    // ============================================================
    // ROOT
    // ============================================================

    public static boolean hasRoot() {
        CommandResult result = execute(
                "id"
        );

        return result.success
                && result.output.contains("uid=0");
    }

    // ============================================================
    // DETECTAR ZRAM
    // ============================================================

    public static boolean exists() {

        File zram = new File(ZRAM_SYS);

        return zram.exists()
                && zram.isDirectory();
    }

    // ============================================================
    // TAMANHO
    // ============================================================

    public static long getConfiguredSizeBytes() {

        File diskSize =
                new File(ZRAM_SYS + "/disksize");

        if (!diskSize.exists()) {
            return 0L;
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new java.io.FileReader(diskSize)
                    );

            String value = reader.readLine();

            reader.close();

            if (value == null) {
                return 0L;
            }

            return Long.parseLong(value.trim());

        } catch (Exception e) {
            return 0L;
        }
    }

    public static boolean is8GiB() {

        return getConfiguredSizeBytes()
                == ZRAM_SIZE_BYTES;
    }

    // ============================================================
    // ALGORITMO
    // ============================================================

    public static String getAvailableAlgorithms() {

        File file =
                new File(
                        ZRAM_SYS
                                + "/comp_algorithm"
                );

        if (!file.exists()) {
            return "";
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new java.io.FileReader(file)
                    );

            String value = reader.readLine();

            reader.close();

            return value == null ? "" : value;

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Tenta selecionar zstd.
     * Caso não exista, tenta lz4.
     * Caso não exista, tenta lzo.
     */
    private static String chooseCompressionAlgorithm() {

        String algorithms =
                getAvailableAlgorithms();

        if (algorithms.contains("zstd")) {
            return "zstd";
        }

        if (algorithms.contains("lz4")) {
            return "lz4";
        }

        if (algorithms.contains("lzo")) {
            return "lzo";
        }

        if (algorithms.contains("lz4hc")) {
            return "lz4hc";
        }

        return "";
    }

    // ============================================================
    // CONFIGURAR 8 GB
    // ============================================================

    public static boolean configure8GiB() {

        if (!hasRoot()) {
            return false;
        }

        if (!exists()) {
            return false;
        }

        /*
         * Primeiro tenta desativar o swap.
         */
        executeRoot(
                "swapoff " + ZRAM_DEVICE
        );

        /*
         * Alguns kernels exigem reset antes
         * de alterar o disksize.
         */
        executeRoot(
                "sh -c 'echo 1 > "
                        + ZRAM_SYS
                        + "/reset'"
        );

        /*
         * Seleciona algoritmo disponível.
         */
        String algorithm =
                chooseCompressionAlgorithm();

        if (!algorithm.isEmpty()) {

            executeRoot(
                    "sh -c 'echo "
                            + algorithm
                            + " > "
                            + ZRAM_SYS
                            + "/comp_algorithm'"
            );
        }

        /*
         * Configura 8 GiB.
         */
        String sizeCommand =
                "sh -c 'echo "
                        + ZRAM_SIZE_BYTES
                        + " > "
                        + ZRAM_SYS
                        + "/disksize'";

        CommandResult sizeResult =
                executeRoot(sizeCommand);

        if (!sizeResult.success) {
            return false;
        }

        /*
         * Formata como swap.
         */
        CommandResult mkswap =
                executeRoot(
                        "mkswap "
                                + ZRAM_DEVICE
                );

        if (!mkswap.success) {
            return false;
        }

        /*
         * Ativa novamente.
         */
        CommandResult swapon =
                executeRoot(
                        "swapon "
                                + ZRAM_DEVICE
                );

        if (!swapon.success) {
            return false;
        }

        /*
         * Confirma o tamanho.
         */
        return is8GiB();
    }

    // ============================================================
    // DESATIVAR
    // ============================================================

    public static boolean disable() {

        if (!hasRoot()) {
            return false;
        }

        CommandResult result =
                executeRoot(
                        "swapoff "
                                + ZRAM_DEVICE
                );

        return result.success;
    }

    // ============================================================
    // INFORMAÇÕES
    // ============================================================

    public static String getStatus() {

        long size =
                getConfiguredSizeBytes();

        if (size <= 0) {
            return "ZRAM: não configurada";
        }

        return "ZRAM: "
                + MemoryInfo.formatBytes(size);
    }

    public static String getDetailedStatus() {

        long size =
                getConfiguredSizeBytes();

        String algorithms =
                getAvailableAlgorithms();

        StringBuilder result =
                new StringBuilder();

        result.append("ZRAM\n");
        result.append("----------------\n");

        result.append("Dispositivo: ")
                .append(ZRAM_DEVICE)
                .append("\n");

        result.append("Tamanho: ")
                .append(
                        MemoryInfo.formatBytes(size)
                )
                .append("\n");

        result.append("Algoritmos: ")
                .append(algorithms)
                .append("\n");

        result.append("8 GiB: ")
                .append(
                        size == ZRAM_SIZE_BYTES
                                ? "SIM"
                                : "NÃO"
                )
                .append("\n");

        result.append("Root: ")
                .append(
                        hasRoot()
                                ? "SIM"
                                : "NÃO"
                );

        return result.toString();
    }

    // ============================================================
    // COMANDOS
    // ============================================================

    private static CommandResult execute(
            String command
    ) {

        return executeCommands(
                false,
                command
        );
    }

    private static CommandResult executeRoot(
            String command
    ) {

        return executeCommands(
                true,
                command
        );
    }

    private static CommandResult executeCommands(
            boolean root,
            String... commands
    ) {

        List<String> output =
                new ArrayList<>();

        List<String> errors =
                new ArrayList<>();

        Process process = null;

        try {

            List<String> command =
                    new ArrayList<>();

            if (root) {
                command.add("su");
                command.add("-c");
            }

            StringBuilder shell =
                    new StringBuilder();

            for (String cmd : commands) {

                if (shell.length() > 0) {
                    shell.append(" && ");
                }

                shell.append(cmd);
            }

            if (root) {
                command.add(shell.toString());
            } else {
                command.add("sh");
                command.add("-c");
                command.add(shell.toString());
            }

            ProcessBuilder builder =
                    new ProcessBuilder(command);

            builder.redirectErrorStream(false);

            process = builder.start();

            BufferedReader stdout =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );

            BufferedReader stderr =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getErrorStream()
                            )
                    );

            String line;

            while ((line = stdout.readLine()) != null) {
                output.add(line);
            }

            while ((line = stderr.readLine()) != null) {
                errors.add(line);
            }

            int exitCode =
                    process.waitFor();

            return new CommandResult(
                    exitCode == 0,
                    exitCode,
                    join(output),
                    join(errors)
            );

        } catch (Exception e) {

            return new CommandResult(
                    false,
                    -1,
                    "",
                    e.getMessage() == null
                            ? "Erro desconhecido"
                            : e.getMessage()
            );

        } finally {

            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String join(
            List<String> lines
    ) {

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            if (result.length() > 0) {
                result.append('\n');
            }

            result.append(line);
        }

        return result.toString();
    }

    // ============================================================
    // RESULTADO
    // ============================================================

    private static final class CommandResult {

        final boolean success;
        final int exitCode;
        final String output;
        final String error;

        CommandResult(
                boolean success,
                int exitCode,
                String output,
                String error
        ) {

            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
    }
}
