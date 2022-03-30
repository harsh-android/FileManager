package filemanager.harshapp.hm.fileexplorer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.cloud.CloudConnection;
import filemanager.harshapp.hm.fileexplorer.misc.IconColorUtils;
import filemanager.harshapp.hm.fileexplorer.misc.IconUtils;
import filemanager.harshapp.hm.fileexplorer.network.NetworkConnection;

import static filemanager.harshapp.hm.fileexplorer.DocumentsApplication.isSpecialDevice;
import static filemanager.harshapp.hm.fileexplorer.provider.CloudStorageProvider.TYPE_CLOUD;

public class TransferAdapter extends CursorViewAdapter<TransferAdapter.ViewHolder> {

    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public TransferAdapter(Context context, Cursor cursor){
        super(context, cursor);
        mContext = context;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.setData(cursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection_list, parent, false);
        return new ViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ViewHolder item, View view, int position);
        void onItemLongClick(ViewHolder item, View view, int position);
        void onItemViewClick(ViewHolder item, View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconMime;
        private final View iconMimeBackground;
        private final TextView summary;
        private final TextView title;
        private final View popupButton;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemClick(ViewHolder.this, v, getLayoutPosition());
                    }
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemLongClick(ViewHolder.this, v, getLayoutPosition());
                    }
                    return false;
                }
            });

            iconMime = (ImageView) v.findViewById(R.id.icon_mime);
            iconMimeBackground = v.findViewById(R.id.icon_mime_background);
            title = (TextView) v.findViewById(android.R.id.title);
            summary = (TextView) v.findViewById(android.R.id.summary);
            popupButton = v.findViewById(R.id.button_popup);
            popupButton.setVisibility(isSpecialDevice() ? View.INVISIBLE : View.VISIBLE);
            popupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(ViewHolder.this, popupButton, getLayoutPosition());
                    }
                }
            });
        }

        public void setData(Cursor cursor){
            NetworkConnection networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
            if(networkConnection.type.startsWith(TYPE_CLOUD)){
                title.setText(CloudConnection.getTypeName(networkConnection.type));
                summary.setText(networkConnection.username);
                iconMimeBackground.setVisibility(View.VISIBLE);
                iconMimeBackground.setBackgroundColor(
                        IconColorUtils.loadCloudColor(mContext, networkConnection.getType()));
                iconMime.setImageDrawable(IconUtils.loadCloudIcon(mContext, networkConnection.type));
            } else {
                title.setText(networkConnection.getName());
                summary.setText(networkConnection.getSummary());
                iconMimeBackground.setVisibility(View.VISIBLE);
                iconMimeBackground.setBackgroundColor(
                        IconColorUtils.loadSchmeColor(mContext, networkConnection.getType()));
                iconMime.setImageDrawable(IconUtils.loadSchemeIcon(mContext, networkConnection.type));
            }
        }
    }
}