package filemanager.harshapp.hm.fileexplorer.cast;

import android.content.Context;
import android.util.AttributeSet;

import androidx.mediarouter.app.MediaRouteButton;
import filemanager.harshapp.hm.fileexplorer.AppPaymentFlavour;

public class CastButton extends MediaRouteButton {
    public CastButton(Context context) {
        super(context);
    }

    public CastButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CastButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean showDialog() {
        if(AppPaymentFlavour.isPurchased()) {
            return super.showDialog();
        } else {
            AppPaymentFlavour.openPurchaseActivity(getContext());
            return true;
        }
    }
}
