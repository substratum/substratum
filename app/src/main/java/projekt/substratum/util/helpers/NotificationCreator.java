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

package projekt.substratum.util.helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;

public class NotificationCreator {

    private Context mContext;
    private String content_title, content_text;
    private Boolean auto_cancel;
    private PendingIntent intent;
    private int small_icon;
    private Bitmap big_icon;
    private int notification_priority, invoke_id;

    public NotificationCreator(Context context, String content_title, String content_text,
                               Boolean auto_cancel, PendingIntent intent, int small_icon,
                               Bitmap big_icon, int notification_priority, int invoke_id) {
        this.mContext = context;
        this.content_title = content_title;
        this.content_text = content_text;
        this.auto_cancel = auto_cancel;
        this.intent = intent;
        this.small_icon = small_icon;
        this.big_icon = big_icon;
        this.notification_priority = notification_priority;
        this.invoke_id = invoke_id;
    }

    public boolean createNotification() {
        try {
            NotificationManager mNotifyManager = (NotificationManager) mContext.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            android.support.v7.app.NotificationCompat.Builder mBuilder = new
                    android.support.v7.app.NotificationCompat.Builder(mContext);

            if (content_title != null) mBuilder.setContentTitle(content_title);
            if (content_text != null) mBuilder.setContentText(content_text);
            if (auto_cancel != null) mBuilder.setAutoCancel(auto_cancel);
            if (intent != null) mBuilder.setContentIntent(intent);
            if (small_icon != 0) mBuilder.setSmallIcon(small_icon);
            if (big_icon != null) mBuilder.setLargeIcon(big_icon);

            mBuilder.setPriority(notification_priority);
            mNotifyManager.notify(invoke_id, mBuilder.build());
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }
}