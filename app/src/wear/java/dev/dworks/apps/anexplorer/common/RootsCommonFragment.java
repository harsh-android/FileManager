package filemanager.harshapp.hm.fileexplorer.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.wear.widget.drawer.WearableCompatDrawerLayout;
import androidx.wear.widget.drawer.WearableDrawerControllerCompat;
import androidx.wear.widget.drawer.WearableNavigationDrawerCompatView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;
import filemanager.harshapp.hm.fileexplorer.BaseActivity;
import filemanager.harshapp.hm.fileexplorer.DocumentsApplication;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.adapter.RootsCommonAdapter;
import filemanager.harshapp.hm.fileexplorer.libcore.util.Objects;
import filemanager.harshapp.hm.fileexplorer.loader.RootsLoader;
import filemanager.harshapp.hm.fileexplorer.misc.AnalyticsManager;
import filemanager.harshapp.hm.fileexplorer.misc.RootsCache;
import filemanager.harshapp.hm.fileexplorer.model.RootInfo;
import filemanager.harshapp.hm.fileexplorer.setting.SettingsActivity;

import static filemanager.harshapp.hm.fileexplorer.fragment.RootsFragment.EXTRA_INCLUDE_APPS;

public class RootsCommonFragment extends BaseFragment
        implements WearableNavigationDrawerView.OnItemSelectedListener{

    private RootsCommonAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Collection<RootInfo>> mCallbacks;

    private WearableNavigationDrawerCompatView mNavigationDrawer;

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

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_roots, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNavigationDrawer = view.findViewById(R.id.navigation_drawer);
        mNavigationDrawer.setBackgroundColor(SettingsActivity.getPrimaryColor());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getActivity();
        final RootsCache roots = DocumentsApplication.getRootsCache(context);
        final BaseActivity.State state = ((BaseActivity) context).getDisplayState();

        WearableCompatDrawerLayout layout = getActivity().findViewById(R.id.drawer_layout);
        WearableDrawerControllerCompat controllerCompat = new WearableDrawerControllerCompat(layout, mNavigationDrawer);
        mNavigationDrawer.setDrawerController(controllerCompat);

        mCallbacks = new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {
            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                return new RootsLoader(context, roots, state);
            }

            @Override
            public void onLoadFinished(
                    Loader<Collection<RootInfo>> loader, Collection<RootInfo> result) {
                if (!isAdded()) return;

                final Intent includeApps = getArguments().getParcelable(EXTRA_INCLUDE_APPS);

                if (mAdapter == null) {
                    mAdapter = new RootsCommonAdapter(context, result, includeApps);
                    mNavigationDrawer.setAdapter(mAdapter);
                    mNavigationDrawer.addOnItemSelectedListener(RootsCommonFragment.this);
                } else {
                    mAdapter.setData(result);
                }
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                mAdapter = null;
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        LoaderManager.getInstance(getAppCompatActivity()).restartLoader(2, null, mCallbacks);
    }

    public void onCurrentRootChanged() {
        if (mAdapter == null || mNavigationDrawer == null) return;
        final RootInfo root = ((BaseActivity) getActivity()).getCurrentRoot();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            final RootInfo item = mAdapter.getItem(i);
            if (item != null) {
                if (Objects.equal(item, root)) {
                    mNavigationDrawer.setCurrentItem(i, true);
                    return;
                }
            }
        }
    }

    @Override
    public void onItemSelected(int position) {
        final BaseActivity activity = BaseActivity.get(RootsCommonFragment.this);
        RootInfo rootInfo = mAdapter.getItem(position);
        if(RootInfo.isProFeature(rootInfo) && !DocumentsApplication.isPurchased()){
            DocumentsApplication.openPurchaseActivity(activity);
            return;
        }
        activity.onRootPicked(rootInfo, true);
        Bundle params = new Bundle();
        params.putString("type", rootInfo.title);
        AnalyticsManager.logEvent("navigate", rootInfo, params);
    }
}
