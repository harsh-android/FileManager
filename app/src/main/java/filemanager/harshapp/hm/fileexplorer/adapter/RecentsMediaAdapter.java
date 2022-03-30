package filemanager.harshapp.hm.fileexplorer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.misc.IconHelper;
import filemanager.harshapp.hm.fileexplorer.model.DocumentInfo;
import filemanager.harshapp.hm.fileexplorer.model.DocumentsContract;
import filemanager.harshapp.hm.fileexplorer.setting.SettingsActivity;

public class RecentsMediaAdapter extends CursorViewAdapter<RecentsMediaAdapter.ViewHolder> {

    private final IconHelper mIconHelper;
    private final int mDefaultColor;
    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public RecentsMediaAdapter(Context context, Cursor cursor, IconHelper iconHelper){
        super(context, cursor);
        mContext = context;
        mDefaultColor = SettingsActivity.getPrimaryColor();
        mIconHelper = iconHelper;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.setData(cursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent, parent, false);
        return new ViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener{
        void onItemClick(ViewHolder item, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconMime;
        private final ImageView iconThumb;
        private final View iconMimeBackground;
        private Cursor mCursor;
        public DocumentInfo mDocumentInfo;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(ViewHolder.this, getLayoutPosition());
                }
            });
            iconMime = (ImageView) v.findViewById(R.id.icon_mime);
            iconThumb = (ImageView) v.findViewById(R.id.icon_thumb);
            iconMimeBackground = v.findViewById(R.id.icon_mime_background);
        }

        public void setData(Cursor cursor){
            mDocumentInfo = DocumentInfo.fromDirectoryCursor(cursor);

            mIconHelper.stopLoading(iconThumb);

            iconMime.animate().cancel();
            iconMime.setAlpha(1f);
            iconThumb.animate().cancel();
            iconThumb.setAlpha(0f);

            final Uri uri = DocumentsContract.buildDocumentUri(mDocumentInfo.authority, mDocumentInfo.documentId);
            mIconHelper.load(uri, mDocumentInfo.path, mDocumentInfo.mimeType,
                    mDocumentInfo.flags, mDocumentInfo.icon, mDocumentInfo.lastModified,
                    iconThumb, iconMime, iconMimeBackground);
        }
    }
}