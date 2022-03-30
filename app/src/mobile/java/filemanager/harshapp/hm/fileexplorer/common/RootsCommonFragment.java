package filemanager.harshapp.hm.fileexplorer.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.fragment.RootsFragment;

public class RootsCommonFragment extends RootsFragment {

    public static void show(FragmentManager fm, Intent includeApps) {
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_INCLUDE_APPS, includeApps);

        final RootsCommonFragment fragment = new RootsCommonFragment();
        fragment.setArguments(args);

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    public static RootsCommonFragment get(FragmentManager fm) {
        return (RootsCommonFragment) fm.findFragmentById(R.id.container_roots);
    }
}