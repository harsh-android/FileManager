package filemanager.harshapp.hm.fileexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import filemanager.harshapp.hm.fileexplorer.AppPaymentFlavour;
import filemanager.harshapp.hm.fileexplorer.DocumentsApplication;
import filemanager.harshapp.hm.fileexplorer.R;

public class ProWrapper extends FrameLayout {

    public ProWrapper(Context context) {
        super(context);
        init(context);
    }

    public ProWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.pro_wrapper, this, true);
        view.findViewById(R.id.action_layout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DocumentsApplication.openPurchaseActivity(getContext());
            }
        });
        setVisibility(AppPaymentFlavour.isPurchased() ? GONE : VISIBLE);
    }

}
