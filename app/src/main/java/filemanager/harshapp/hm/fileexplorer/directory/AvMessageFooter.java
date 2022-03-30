package filemanager.harshapp.hm.fileexplorer.directory;

import filemanager.harshapp.hm.fileexplorer.directory.DocumentsAdapter.Environment;

public class AvMessageFooter extends Footer {

    public AvMessageFooter(Environment environment, int itemViewType, int icon, String message) {
        super(itemViewType);
        mIcon = icon;
        mMessage = message;
        mEnv = environment;
    }
}