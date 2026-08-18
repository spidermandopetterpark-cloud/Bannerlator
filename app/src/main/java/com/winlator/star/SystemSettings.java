package com.winlator.star;

import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.star.R;

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

        ramSeekBar.setMax(4096);
        ramSeekBar.setProgress(ramMB);

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
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        swapSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        swapMB = progress;
                        updateUI();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

        autoMemory.setOnCheckedChangeListener(
                (buttonView, checked) -> {

                    if (checked) {
                        long available = nativeGetAvailableMemoryMB();

                        ramMB = Math.min(
                                Math.max(1024, available / 2),
                                4096
                        );

                        swapMB = ramMB;

                        ramSeekBar.setProgress(ramMB);
                        swapSeekBar.setProgress(swapMB);

                        updateUI();
                    }
                });

        applyButton.setOnClickListener(v -> {

            nativeSetMemorySizeMB(ramMB);
            nativeSetSwapSizeMB(swapMB);

            finish();
        });
    }

    private void updateUI() {

        ramText.setText(
                "RAM: " + ramMB + " MB"
        );

        swapText.setText(
                "Swap: " + swapMB + " MB"
        );

        long available = nativeGetAvailableMemoryMB();

        availableText.setText(
                "RAM disponível: " + available + " MB"
        );
    }

    private native long nativeGetAvailableMemoryMB();

    private native boolean nativeSetMemorySizeMB(int memoryMB);

    private native boolean nativeSetSwapSizeMB(int swapMB);
}
