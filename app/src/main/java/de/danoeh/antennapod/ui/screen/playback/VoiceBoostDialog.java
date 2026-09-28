package de.danoeh.antennapod.ui.screen.playback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.internal.MediaLibrarySessionCallback;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class VoiceBoostDialog extends BottomSheetDialogFragment {
    public static final String TAG = "VoiceBoostDialog";

    private MaterialSwitch switchVoiceBoost;
    private LinearLayout gainContainer;
    private TextView txtGainValue;
    private Slider sliderGain;
    private ChipGroup chipGroupPresets;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_voice_boost, container, false);
        switchVoiceBoost = root.findViewById(R.id.switchVoiceBoost);
        gainContainer = root.findViewById(R.id.gainContainer);
        txtGainValue = root.findViewById(R.id.txtGainValue);
        sliderGain = root.findViewById(R.id.sliderGain);
        chipGroupPresets = root.findViewById(R.id.chipGroupPresets);

        boolean isEnabled = UserPreferences.isVoiceBoost();
        int currentGainDb = UserPreferences.getVoiceBoostGainDb();

        switchVoiceBoost.setChecked(isEnabled);
        updateGainContainerVisibility(isEnabled);

        sliderGain.setValue(currentGainDb);
        updateGainLabel(currentGainDb);
        selectMatchingPresetChip(currentGainDb);

        switchVoiceBoost.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserPreferences.setVoiceBoost(isChecked);
            updateGainContainerVisibility(isChecked);
            sendVoiceBoostUpdate(isChecked, (int) sliderGain.getValue());
        });

        sliderGain.addOnChangeListener((slider, value, fromUser) -> {
            int gain = (int) value;
            updateGainLabel(gain);
            if (fromUser) {
                UserPreferences.setVoiceBoostGainDb(gain);
                selectMatchingPresetChip(gain);
                if (switchVoiceBoost.isChecked()) {
                    sendVoiceBoostUpdate(true, gain);
                }
            }
        });

        chipGroupPresets.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            int targetDb = -1;
            if (id == R.id.chipPresetMild) {
                targetDb = 3;
            } else if (id == R.id.chipPresetBalanced) {
                targetDb = 6;
            } else if (id == R.id.chipPresetHigh) {
                targetDb = 9;
            } else if (id == R.id.chipPresetMax) {
                targetDb = 12;
            }
            if (targetDb >= 0 && targetDb != (int) sliderGain.getValue()) {
                sliderGain.setValue(targetDb);
                UserPreferences.setVoiceBoostGainDb(targetDb);
                updateGainLabel(targetDb);
                if (switchVoiceBoost.isChecked()) {
                    sendVoiceBoostUpdate(true, targetDb);
                }
            }
        });

        return root;
    }

    private void updateGainContainerVisibility(boolean isEnabled) {
        gainContainer.setAlpha(isEnabled ? 1.0f : 0.4f);
        sliderGain.setEnabled(isEnabled);
        for (int i = 0; i < chipGroupPresets.getChildCount(); i++) {
            chipGroupPresets.getChildAt(i).setEnabled(isEnabled);
        }
    }

    private void updateGainLabel(int gainDb) {
        txtGainValue.setText(getString(R.string.voice_boost_gain_format, gainDb));
    }

    private void selectMatchingPresetChip(int gainDb) {
        if (gainDb == 3) {
            chipGroupPresets.check(R.id.chipPresetMild);
        } else if (gainDb == 6) {
            chipGroupPresets.check(R.id.chipPresetBalanced);
        } else if (gainDb == 9) {
            chipGroupPresets.check(R.id.chipPresetHigh);
        } else if (gainDb == 12) {
            chipGroupPresets.check(R.id.chipPresetMax);
        } else {
            chipGroupPresets.clearCheck();
        }
    }

    private void sendVoiceBoostUpdate(boolean enabled, int gainDb) {
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            android.os.Bundle bundle = MediaLibrarySessionCallback.createBundle(enabled);
            bundle.putInt("value", gainDb);
            PlaybackController.bindToMedia3Service(getContext(), mediaController ->
                    mediaController.sendCustomCommand(MediaLibrarySessionCallback.SESSION_COMMAND_VOICE_BOOST, bundle));
        }
    }
}
