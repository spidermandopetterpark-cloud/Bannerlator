package com.winlator.star;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SystemSettings extends AppCompatActivity {

    static {
        System.loadLibrary("titanpc");
    }

    private SeekBar ramSeekBar;
    private SeekBar swapSeekBar;

    private TextView ramText;
    private TextView swapText;
    private TextView availableText;

    private Switch autoMemory;
    private Button applyButton;

    private int ramMB = 2048;
    private int swapMB = 2048;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_system_settings);

        ramSeekBar = findViewById(R.id.ramSeekBar);
        swapSeekBar = findViewById(R.id.swapSeekBar);

        ramText = findViewById(R.id.ramText);
        swapText = findViewById(R.id.swapText);
        availableText = findViewById(R.id.availableText);

        autoMemory = findViewById(R.id.autoMemory);
        applyButton = findViewById(R.id.applyButton);

        /*
         * RAM:
         * 512 MB até 4096 MB
         */
        ramSeekBar.setMax(4096);
        ramSeekBar.setProgress(ramMB);

        /*
         * Swap:
         * 0 MB até 8192 MB
         */
        swapSeekBar.setMax(8192);
        swapSeekBar.setProgress(swapMB);

        updateUI();

        ramSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        ramMB = Math.max(512, progress);

                        updateUI();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );

        swapSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        swapMB = Math.max(0, progress);

                        updateUI();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );

        autoMemory.setOnCheckedChangeListener(
                (buttonView, checked) -> {

                    if (!checked) {
                        return;
                    }

                    /*
                     * nativeGetAvailableMemoryMB()
                     * retorna LONG.
                     *
                     * Fazemos todo o cálculo como long
                     * e só convertemos para int no final.
                     */
                    long available =
                            nativeGetAvailableMemoryMB();

                    long automaticRam =
                            available / 2L;

                    if (automaticRam < 1024L) {
                        automaticRam = 1024L;
                    }

                    if (automaticRam > 4096L) {
                        automaticRam = 4096L;
                    }

                    ramMB = (int) automaticRam;

                    /*
                     * Swap automático = RAM configurada.
                     */
                    swapMB = ramMB;

                    ramSeekBar.setProgress(ramMB);
                    swapSeekBar.setProgress(swapMB);

                    updateUI();
                }
        );

        applyButton.setOnClickListener(
                v -> {

                    /*
                     * Os métodos JNI recebem int,
                     * portanto ramMB e swapMB também são int.
                     */
                    nativeSetMemorySizeMB(ramMB);

                    nativeSetSwapSizeMB(swapMB);

                    updateUI();
                }
        );
    }

    private void updateUI() {

        ramText.setText(
                "RAM: " + ramMB + " MB"
        );

        swapText.setText(
                "Swap: " + swapMB + " MB"
        );

        long available =
                nativeGetAvailableMemoryMB();

        availableText.setText(
                "RAM disponível: " +
                        available +
                        " MB"
        );
    }

    /*
     * Retorna a RAM disponível do Android/Linux.
     */
    private native long nativeGetAvailableMemoryMB();

    /*
     * Configura a RAM do container.
     */
    private native boolean nativeSetMemorySizeMB(
            int memoryMB
    );

    /*
     * Configura o tamanho lógico do Swap.
     */
    private native boolean nativeSetSwapSizeMB(
            int swapMB
    );
}
