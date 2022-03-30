package filemanager.harshapp.hm.fileexplorer.cast;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.cast.framework.R.id;
import com.google.android.gms.cast.framework.media.widget.MiniControllerFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import filemanager.harshapp.hm.fileexplorer.misc.TintUtils;
import filemanager.harshapp.hm.fileexplorer.setting.SettingsActivity;

public class CastMiniController extends MiniControllerFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int color = SettingsActivity.getAccentColor();
        ProgressBar progressBar = view.findViewById(id.progressBar);
        TintUtils.tintDrawable(progressBar.getProgressDrawable(), color);
    }
}
