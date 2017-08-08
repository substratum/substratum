/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.services.notification;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;

public class CompileDialogThread extends Thread implements Runnable {

    private final Object mutex = new Object();
    public Handler handler = null;
    public String packageCodePath = "";
    public String LOGTAG = "";
    public File mAppRoot = null;
    private Context mContext;
    private HandlerThread handlerThread = new HandlerThread(LOGTAG);

    public CompileDialogThread() {
    }

    void pause(int milli) {
        try {
            Thread.sleep(milli);
        } catch (Exception ex) {
            Log.d(LOGTAG, "dialog test failed");
        }
    }

    protected void reply(int arg1, int arg2, String text) {
        Message msg = new Message();
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = (Object) text;
        if (handler != null) {
            handler.sendMessage(msg);
        }
    }

    @Override
    public void run() {
        // Create and start the HandlerThread- it requires a custom name
        HandlerThread handlerThread = new HandlerThread(LOGTAG);
        handlerThread.start();

        // Get the looper from the handlerThread
        // Note: this may return null
        Looper looper = handlerThread.getLooper();

        // Create a new handler- passing in the looper to use and this class as
        // the message handler
        Handler handler = new Handler(looper);

        // While this thread is running

        // TODO- custom thread logic

        // Wait on mutex
        synchronized (mutex) {
            mutex.notifyAll();

            try {

                reply(1, 0, "Checking Overlays...");
                pause(1000);
                reply(1, 0, "Stoping target processes...");
                pause(1000);
                reply(1, 25, "Sending to Phase2 (wtf is that?)...");
                pause(1000);
                reply(1, 50, "Phase 3 maybe?...");
                pause(1000);
                reply(1, 75, "Preparing Installation...");
                pause(1000);
                reply(1, 90, "Cleaning Up...");
                pause(1000);
                reply(0, 0, "Phase complete");
            } catch (Exception ex) {
                reply(0, 1, ex.getMessage());
            }
        }
    }
}