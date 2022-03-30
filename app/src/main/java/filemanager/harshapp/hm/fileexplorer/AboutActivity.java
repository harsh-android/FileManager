/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
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

package filemanager.harshapp.hm.fileexplorer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import filemanager.harshapp.hm.fileexplorer.misc.AnalyticsManager;
import filemanager.harshapp.hm.fileexplorer.misc.ColorUtils;
import filemanager.harshapp.hm.fileexplorer.misc.SystemBarTintManager;
import filemanager.harshapp.hm.fileexplorer.misc.Utils;
import filemanager.harshapp.hm.fileexplorer.setting.SettingsActivity;

import static filemanager.harshapp.hm.fileexplorer.DocumentsActivity.getStatusBarHeight;
import static filemanager.harshapp.hm.fileexplorer.misc.Utils.getSuffix;
import static filemanager.harshapp.hm.fileexplorer.misc.Utils.openFeedback;
import static filemanager.harshapp.hm.fileexplorer.misc.Utils.openPlaystore;

public class AboutActivity extends AboutVariantFlavour implements View.OnClickListener {

	public static final String TAG = "About";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Utils.hasKitKat() && !Utils.hasLollipop()){
			setTheme(R.style.DocumentsTheme_Translucent);
		}
		setContentView(R.layout.activity_about);

		int color = SettingsActivity.getPrimaryColor();
		View view = findViewById(R.id.toolbar);
		if(view instanceof Toolbar){
			Toolbar mToolbar = (Toolbar) view;
			mToolbar.setTitleTextAppearance(this, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
			if(Utils.hasKitKat() && !Utils.hasLollipop()) {
				mToolbar.setPadding(0, getStatusBarHeight(this), 0, 0);
			}
			mToolbar.setBackgroundColor(color);
			setSupportActionBar(mToolbar);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle(null);
			setUpDefaultStatusBar();
		} else {
			view.setBackgroundColor(color);
		}
		initAd();
		initControls();
	}

	@Override
	public String getTag() {
		return TAG;
	}

	private void initControls() {

		int accentColor = ColorUtils.getTextColorForBackground(SettingsActivity.getPrimaryColor());
		TextView logo = (TextView)findViewById(R.id.logo);
		logo.setTextColor(accentColor);
		String header = logo.getText() + getSuffix() + " v" + BuildConfig.VERSION_NAME + (BuildConfig.DEBUG ? " Debug" : "");
		logo.setText(header);

		TextView action_rate = (TextView)findViewById(R.id.action_rate);
		TextView action_support = (TextView)findViewById(R.id.action_support);
		TextView action_share = (TextView)findViewById(R.id.action_share);
		TextView action_feedback = (TextView)findViewById(R.id.action_feedback);
		TextView action_sponsor = (TextView)findViewById(R.id.action_sponsor);

		action_rate.setOnClickListener(this);
		action_support.setOnClickListener(this);
		action_share.setOnClickListener(this);
		action_feedback.setOnClickListener(this);
		action_sponsor.setOnClickListener(this);

		if(Utils.isOtherBuild()){
			action_rate.setVisibility(View.GONE);
			action_support.setVisibility(View.GONE);
		} else if(DocumentsApplication.isTelevision()){
			action_share.setVisibility(View.GONE);
			action_feedback.setVisibility(View.GONE);
		}

		if(!DocumentsApplication.isPurchased()){
			action_sponsor.setVisibility(View.VISIBLE);
		}
	}

    @Override
    public void startActivity(Intent intent) {
        if(Utils.isIntentAvailable(this, intent)) {
            super.startActivity(intent);
        }
    }

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.action_feedback:
				openFeedback(this);
				break;
			case R.id.action_rate:
				openPlaystore(this);
				AnalyticsManager.logEvent("app_rate");
				break;
			case R.id.action_sponsor:
				showAd();
				AnalyticsManager.logEvent("app_sponsor");
				break;
			case R.id.action_support:
				if(Utils.isProVersion()){
					Intent intentMarketAll = new Intent("android.intent.action.VIEW");
					intentMarketAll.setData(Utils.getAppProStoreUri());
					startActivity(intentMarketAll);
				} else {
					DocumentsApplication.openPurchaseActivity(this);
				}
				AnalyticsManager.logEvent("app_love");
				break;
			case R.id.action_share:

				/*String shareText = "I found this file mananger very useful. Give it a try. "
						+ Utils.getAppShareUri().toString();
				ShareCompat.IntentBuilder
						.from(this)
						.setText(shareText)
						.setType("text/plain")
						.setChooserTitle("Share AnExplorer")
						.startChooser();*/
				try{

					Intent shareIntent;
					Bitmap bitmap= BitmapFactory.decodeResource(getResources(),R.drawable.cover);
					String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/Share.png";
					OutputStream out = null;
					File file=new File(path);
					try {
						out = new FileOutputStream(file);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
						out.flush();
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					path=file.getPath();
					Uri bmpUri = Uri.parse("file://"+path);
					shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
					shareIntent.putExtra(Intent.EXTRA_TEXT,"*AV File Manager* for Display All Videos and Images." + "\n\n1. Display All Images\uD83D\uDCF7 .\n2. Display All Videos \uD83C\uDF9E.\n3. \uD83D\uDCE1 Connect Phone \uD83D\uDCF1 to PC \uD83D\uDDA5.\n4. Display All Documents \uD83D\uDCC4.\n5. Display All Music \uD83D\uDCFB.\n6. Connect USB.\n7. Clear Your Memory \uD83D\uDCBE.\n8. It's Check Your Phone is Root or Not \uD83D\uDCA3. \n\n \uD83D\uDC49\uD83C\uDFFB Please Install This App and on \uD83D\uDCF1\n\n Give 5 \uD83C\uDF1F Rating With Review ✒\n\n\n\uD83D\uDC47\uD83C\uDFFB \uD83D\uDC47\uD83C\uDFFB \uD83D\uDC47\uD83C\uDFFB \uD83D\uDC47\uD83C\uDFFB \n\n" + "https://play.google.com/store/apps/details?id=" +getPackageName() + "\n \uD83D\uDE4F \uD83D\uDE4F \uD83D\uDE4F");
					shareIntent.setType("image/*");
					startActivity(Intent.createChooser(shareIntent,"Share App Link Using..."));


				}catch (Exception e){

				}
				AnalyticsManager.logEvent("app_share");
				break;
		}
	}

	public void setUpDefaultStatusBar() {
		int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
		if(Utils.hasLollipop()){
			getWindow().setStatusBarColor(color);
		}
		else if(Utils.hasKitKat()){
			SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
			systemBarTintManager.setTintColor(Utils.getStatusBarColor(color));
			systemBarTintManager.setStatusBarTintEnabled(true);
		}
	}
}