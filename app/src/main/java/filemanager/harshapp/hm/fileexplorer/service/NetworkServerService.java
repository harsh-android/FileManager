package filemanager.harshapp.hm.fileexplorer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import filemanager.harshapp.hm.fileexplorer.misc.CrashReportingManager;
import filemanager.harshapp.hm.fileexplorer.model.RootInfo;
import filemanager.harshapp.hm.fileexplorer.network.NetworkConnection;
import filemanager.harshapp.hm.fileexplorer.network.NetworkServiceHandler;

import static filemanager.harshapp.hm.fileexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_FAILEDTOSTART;
import static filemanager.harshapp.hm.fileexplorer.misc.Utils.EXTRA_ROOT;

public abstract class NetworkServerService extends Service {

    private static final String TAG = NetworkServerService.class.getSimpleName();
    public static final int MSG_START = 1;
    public static final int MSG_STOP = 2;

    private Looper serviceLooper;
    private NetworkServiceHandler serviceHandler;
    private NetworkConnection networkConnection;
    private RootInfo root;

    protected abstract NetworkServiceHandler createServiceHandler(
            Looper serviceLooper,
            NetworkServerService service);

    public abstract Object getServer();

    public abstract boolean launchServer();

    public abstract void stopServer();

    protected void handleServerStartError(Exception e) {
        CrashReportingManager.logException(e);
        sendBroadcast(new Intent(ACTION_FTPSERVER_FAILEDTOSTART));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(
                "ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = createServiceHandler(serviceLooper, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_REDELIVER_INTENT;
        }

        // get parameters
        Bundle extras = intent.getExtras();
        if(null != extras) {
            root = extras.getParcelable(EXTRA_ROOT);
        }
        if(null == root){
            networkConnection = NetworkConnection.getDefaultServer(getApplicationContext());
        } else {
            networkConnection = NetworkConnection.fromRootInfo(getApplicationContext(), root);
        }
        // send start message (to handler)
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = MSG_START;
        serviceHandler.sendMessage(msg);


        // we don't want the system to kill the ftp server
        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // send stop message (to handler)
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = MSG_STOP;
        serviceHandler.sendMessage(msg);
    }

    public RootInfo getRootInfo() {
        return root;
    }

    public NetworkConnection getNetworkConnection() {
        return networkConnection;
    }
}
