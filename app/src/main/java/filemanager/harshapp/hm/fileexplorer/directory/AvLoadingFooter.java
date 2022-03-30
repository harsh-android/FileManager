package filemanager.harshapp.hm.fileexplorer.directory;

import filemanager.harshapp.hm.fileexplorer.directory.DocumentsAdapter.Environment;

public class AvLoadingFooter extends Footer {

    public AvLoadingFooter(Environment environment, int type) {
        super(type);
        mEnv = environment;
        mIcon = 0;
        mMessage = "";
    }
}