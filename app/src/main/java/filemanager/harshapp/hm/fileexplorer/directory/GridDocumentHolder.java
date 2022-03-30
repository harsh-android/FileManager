package filemanager.harshapp.hm.fileexplorer.directory;

import android.content.Context;
import android.view.ViewGroup;

import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import filemanager.harshapp.hm.fileexplorer.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
