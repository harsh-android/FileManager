package filemanager.harshapp.hm.fileexplorer.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import filemanager.harshapp.hm.fileexplorer.libcore.io.IoUtils;
import filemanager.harshapp.hm.fileexplorer.misc.ContentProviderClientCompat;

import static filemanager.harshapp.hm.fileexplorer.BaseActivity.State.MODE_UNKNOWN;
import static filemanager.harshapp.hm.fileexplorer.BaseActivity.State.SORT_ORDER_UNKNOWN;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        IoUtils.closeQuietly(cursor);
        ContentProviderClientCompat.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}