/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package filemanager.harshapp.hm.fileexplorer.fragment;

import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;
import filemanager.harshapp.hm.fileexplorer.BaseActivity;
import filemanager.harshapp.hm.fileexplorer.DocumentsApplication;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.common.DialogBuilder;
import filemanager.harshapp.hm.fileexplorer.common.DialogFragment;
import filemanager.harshapp.hm.fileexplorer.misc.AsyncTask;
import filemanager.harshapp.hm.fileexplorer.misc.ContentProviderClientCompat;
import filemanager.harshapp.hm.fileexplorer.misc.CrashReportingManager;
import filemanager.harshapp.hm.fileexplorer.misc.ProviderExecutor;
import filemanager.harshapp.hm.fileexplorer.misc.TintUtils;
import filemanager.harshapp.hm.fileexplorer.misc.Utils;
import filemanager.harshapp.hm.fileexplorer.model.DocumentInfo;
import filemanager.harshapp.hm.fileexplorer.model.DocumentsContract;
import filemanager.harshapp.hm.fileexplorer.model.DocumentsContract.Document;

import static filemanager.harshapp.hm.fileexplorer.DocumentsActivity.TAG;

/**
 * Dialog to create a new directory.
 */
public class CreateDirectoryFragment extends DialogFragment {
    private static final String TAG_CREATE_DIRECTORY = "create_directory";

    public static void show(FragmentManager fm) {
        final CreateDirectoryFragment dialog = new CreateDirectoryFragment();
        dialog.show(fm, TAG_CREATE_DIRECTORY);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final DialogBuilder builder = new DialogBuilder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(context);

        final View view = dialogInflater.inflate(R.layout.dialog_create_dir, null, false);
        final EditText text1 = (EditText) view.findViewById(android.R.id.text1);
        TintUtils.tintWidget(text1);

        builder.setTitle(R.string.menu_create_dir);
        builder.setView(view);

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String displayName = text1.getText().toString();

                final BaseActivity activity = (BaseActivity) getActivity();
                final DocumentInfo cwd = activity.getCurrentDirectory();

                if(TextUtils.isEmpty(displayName)){
                    Utils.showError(activity, R.string.create_error);
                    return;
                }
                new CreateDirectoryTask(activity, cwd, displayName).executeOnExecutor(
                        ProviderExecutor.forAuthority(cwd.authority));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
    
    private class CreateDirectoryTask extends AsyncTask<Void, Void, DocumentInfo> {
        private final BaseActivity mActivity;
        private final DocumentInfo mCwd;
		private final String mDisplayName;

        public CreateDirectoryTask(
                BaseActivity activity, DocumentInfo cwd, String displayName) {
            mActivity = activity;
            mCwd = cwd;
            mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            final ContentResolver resolver = mActivity.getContentResolver();
            ContentProviderClient client = null;
            try {
				client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mCwd.derivedUri.getAuthority());
                final Uri childUri = DocumentsContract.createDocument(
                		resolver, mCwd.derivedUri, Document.MIME_TYPE_DIR, mDisplayName);
                return DocumentInfo.fromUri(resolver, childUri);
            } catch (Exception e) {
                Log.w(TAG, "Failed to create directory", e);
                CrashReportingManager.logException(e);
                return null;
            } finally {
            	ContentProviderClientCompat.releaseQuietly(client);
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (result != null) {
                // Navigate into newly created child
                mActivity.onDocumentPicked(result);
            } else {
                if(!mActivity.isSAFIssue(mCwd.documentId)) {
                    Utils.showError(mActivity, R.string.create_error);
                }
            }

            mActivity.setPending(false);
        }
    }
}
