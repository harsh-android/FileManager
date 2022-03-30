package filemanager.harshapp.hm.fileexplorer.directory;

import android.content.Context;
import android.view.ViewGroup;

import filemanager.harshapp.hm.fileexplorer.R;

import static filemanager.harshapp.hm.fileexplorer.BaseActivity.State.MODE_GRID;

public class AvLoadingHolder extends BaseHolder {

    public AvLoadingHolder(DocumentsAdapter.Environment environment, Context context, ViewGroup parent) {
        super(context, parent, getLayoutId(environment));
    }

    public static int getLayoutId(DocumentsAdapter.Environment environment){
        int layoutId = R.layout.item_loading_list;
        if(environment.getDisplayState().derivedMode == MODE_GRID){
            layoutId = R.layout.item_loading_grid;
        }
        return layoutId;
    }
}
