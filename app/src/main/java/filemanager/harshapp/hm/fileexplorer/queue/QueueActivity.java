/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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

package filemanager.harshapp.hm.fileexplorer.queue;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.framework.CastButtonFactory;

import androidx.appcompat.widget.Toolbar;
import filemanager.harshapp.hm.fileexplorer.DocumentsApplication;
import filemanager.harshapp.hm.fileexplorer.R;
import filemanager.harshapp.hm.fileexplorer.cast.Casty;
import filemanager.harshapp.hm.fileexplorer.common.ActionBarActivity;
import filemanager.harshapp.hm.fileexplorer.misc.SystemBarTintManager;
import filemanager.harshapp.hm.fileexplorer.misc.Utils;
import filemanager.harshapp.hm.fileexplorer.setting.SettingsActivity;

/**
 * An activity to show the queue list
 */
public class QueueActivity extends ActionBarActivity {

    private static final String TAG = "QueueActivity";
    private Casty casty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);


        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");

        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        if (savedInstanceState == null) {
            QueueFragment.show(getSupportFragmentManager());
        }

        setupActionBar();
        casty = DocumentsApplication.getInstance().getCasty();
    }

    private void setupActionBar() {
        int color = SettingsActivity.getPrimaryColor();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.queue_list);
        toolbar.setBackgroundColor(color);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setUpDefaultStatusBar();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpDefaultStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(color);
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_cast, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.casty_media_route_menu_item);
        return true;
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
