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

package projekt.substratum.common.systems;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.OverlayInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayType1a;
import static projekt.substratum.common.References.metadataOverlayType1b;
import static projekt.substratum.common.References.metadataOverlayType1c;
import static projekt.substratum.common.References.metadataOverlayType2;
import static projekt.substratum.common.References.metadataOverlayType3;

public class ProfileManager {
    public static final String SCHEDULED_PROFILE_ENABLED = "scheduled_profile_enabled";
    public static final String SCHEDULED_PROFILE_TYPE_EXTRA = "type";
    public static final String SCHEDULED_PROFILE_CURRENT_PROFILE = "current_profile";
    public static final String NIGHT = "night";
    public static final String NIGHT_PROFILE = "night_profile";
    public static final String NIGHT_PROFILE_HOUR = "night_profile_hour";
    public static final String NIGHT_PROFILE_MINUTE = "night_profile_minute";
    public static final String DAY_PROFILE = "day_profile";
    public static final String DAY_PROFILE_HOUR = "day_profile_hour";
    public static final String DAY_PROFILE_MINUTE = "day_profile_minute";
    // Profile state list tags
    private static final String METADATA_PROFILE_ENABLED = "enabled";
    private static final String METADATA_PROFILE_DISABLED = "disabled";
    private static final String METADATA_PROFILE_OVERLAYS = "overlays";
    private static final String METADATA_PROFILE_ITEM = "item";
    private static final String METADATA_PROFILE_PACKAGE_NAME = "packageName";
    private static final String METADATA_PROFILE_TARGET = "target";
    private static final String METADATA_PROFILE_PARENT = "parent";
    private static final String METADATA_PROFILE_TYPE1A = "type1a";
    private static final String METADATA_PROFILE_TYPE1B = "type1b";
    private static final String METADATA_PROFILE_TYPE1C = "type1c";
    private static final String METADATA_PROFILE_TYPE2 = "type2";
    private static final String METADATA_PROFILE_TYPE3 = "type3";
    private static final String DAY = "day";

    public static void updateScheduledProfile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String currentProfile = prefs.getString(SCHEDULED_PROFILE_CURRENT_PROFILE, "");
        final int dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
        final int dayMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
        final int nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
        final int nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);

        // Set up current calendar instance
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());

        // Set up day night calendar instance
        Calendar calendarNight = Calendar.getInstance();
        calendarNight.setTimeInMillis(System.currentTimeMillis());
        calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
        calendarNight.set(Calendar.MINUTE, nightMinute);
        calendarNight.set(Calendar.SECOND, 0);

        Calendar calendarDay = Calendar.getInstance();
        calendarDay.setTimeInMillis(System.currentTimeMillis());
        calendarDay.set(Calendar.HOUR_OF_DAY, dayHour);
        calendarDay.set(Calendar.MINUTE, dayMinute);
        calendarDay.set(Calendar.SECOND, 0);

        // night time
        if (currentProfile.equals(NIGHT)) {
            calendarNight.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (alarmMgr != null) {
            alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendarNight.getTimeInMillis(), nightIntent);
        }

        // Bring back the day in case we went to the conditional if before
        calendarNight.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));

        // day time
        if (currentProfile.equals(DAY) || current.after(calendarNight)) {
            calendarDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (alarmMgr != null) {
            alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendarDay.getTimeInMillis(), dayIntent);
        }
    }

    public static void enableScheduledProfile(Context context, String dayProfile,
                                              int dayHour, int dayMinute, String nightProfile,
                                              int nightHour, int nightMinute) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set up current calendar instance
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());

        // Set up day night calendar instance
        Calendar calendarNight = Calendar.getInstance();
        calendarNight.setTimeInMillis(System.currentTimeMillis());
        calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
        calendarNight.set(Calendar.MINUTE, nightMinute);
        calendarNight.set(Calendar.SECOND, 0);

        Calendar calendarDay = Calendar.getInstance();
        calendarDay.setTimeInMillis(System.currentTimeMillis());
        calendarDay.set(Calendar.HOUR_OF_DAY, dayHour);
        calendarDay.set(Calendar.MINUTE, dayMinute);
        calendarDay.set(Calendar.SECOND, 0);

        // Apply night profile
        if (calendarDay.after(current) && calendarNight.after(current)) {
            // We will go here when we apply in night profile time on different day,
            // make sure we apply the night profile directly
            calendarNight.add(Calendar.DAY_OF_YEAR, -1);
        }
        if (alarmMgr != null) {
            alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendarNight.getTimeInMillis(), nightIntent);
        }

        // Bring back the day in case we went to the conditional if before
        calendarNight.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));

        // Apply day profile
        if (calendarNight.before(current)) {
            // We will go here when we apply inside night profile time, this prevent day profile
            // to be triggered
            calendarDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (alarmMgr != null) {
            alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendarDay.getTimeInMillis(), dayIntent);
        }

        // Apply prefs
        editor.putBoolean(SCHEDULED_PROFILE_ENABLED, true);
        editor.putString(NIGHT_PROFILE, nightProfile);
        editor.putString(DAY_PROFILE, dayProfile);
        editor.putInt(NIGHT_PROFILE_HOUR, nightHour);
        editor.putInt(NIGHT_PROFILE_MINUTE, nightMinute);
        editor.putInt(DAY_PROFILE_HOUR, dayHour);
        editor.putInt(DAY_PROFILE_MINUTE, dayMinute);
        editor.apply();
    }

    public static void disableScheduledProfile(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        PendingIntent nightIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        PendingIntent dayIntent =
                PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmMgr != null) {
            alarmMgr.cancel(nightIntent);
            alarmMgr.cancel(dayIntent);

            editor.remove(SCHEDULED_PROFILE_ENABLED);
            editor.remove(DAY_PROFILE);
            editor.remove(DAY_PROFILE_HOUR);
            editor.remove(DAY_PROFILE_MINUTE);
            editor.remove(NIGHT_PROFILE);
            editor.remove(NIGHT_PROFILE_HOUR);
            editor.remove(NIGHT_PROFILE_MINUTE);
            editor.apply();
        }
    }

    public static void writeProfileState(Context context, String profileName) {
        try {
            try (FileOutputStream outputStream = new FileOutputStream(
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + profileName + "/overlay_state.xml")) {
                XmlSerializer xmlSerializer = Xml.newSerializer();
                xmlSerializer.setOutput(outputStream, "UTF-8");
                xmlSerializer.setFeature(
                        "http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializer.startDocument(null, true);
                xmlSerializer.startTag(null, METADATA_PROFILE_OVERLAYS);

                // Write enabled overlays
                List<String> enabled = ThemeManager.listOverlays(context, STATE_APPROVED_ENABLED);
                if (enabled.size() > 0) {
                    xmlSerializer.startTag(null, METADATA_PROFILE_ENABLED);
                    for (String packageName : enabled) {
                        String target = References.getOverlayMetadata(
                                context, packageName, metadataOverlayTarget);
                        String parent = References.getOverlayMetadata(
                                context, packageName, metadataOverlayParent);
                        String type1a = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1a);
                        String type1b = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1b);
                        String type1c = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1c);
                        String type2 = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType2);
                        String type3 = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType3);

                        xmlSerializer.startTag(null, METADATA_PROFILE_ITEM)
                                .attribute(null, METADATA_PROFILE_PACKAGE_NAME, packageName)
                                .attribute(null, METADATA_PROFILE_TARGET, target)
                                .attribute(null, METADATA_PROFILE_PARENT, parent)
                                .attribute(null, METADATA_PROFILE_TYPE1A, type1a)
                                .attribute(null, METADATA_PROFILE_TYPE1B, type1b)
                                .attribute(null, METADATA_PROFILE_TYPE1C, type1c)
                                .attribute(null, METADATA_PROFILE_TYPE2, type2)
                                .attribute(null, METADATA_PROFILE_TYPE3, type3)
                                .endTag(null, METADATA_PROFILE_ITEM);
                    }
                    xmlSerializer.endTag(null, METADATA_PROFILE_ENABLED);
                }

                // Write disabled overlays
                List<String> disabled = ThemeManager.listOverlays(context, STATE_APPROVED_DISABLED);
                if (disabled.size() > 0) {
                    xmlSerializer.startTag(null, METADATA_PROFILE_DISABLED);
                    for (String packageName : disabled) {
                        String target = References.getOverlayMetadata(
                                context, packageName, metadataOverlayTarget);
                        String parent = References.getOverlayMetadata(
                                context, packageName, metadataOverlayParent);
                        String type1a = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1a);
                        String type1b = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1b);
                        String type1c = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType1c);
                        String type2 = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType2);
                        String type3 = References.getOverlayMetadata(
                                context, packageName, metadataOverlayType3);

                        xmlSerializer.startTag(null, METADATA_PROFILE_ITEM)
                                .attribute(null, METADATA_PROFILE_PACKAGE_NAME, packageName)
                                .attribute(null, METADATA_PROFILE_TARGET, target)
                                .attribute(null, METADATA_PROFILE_PARENT, parent)
                                .attribute(null, METADATA_PROFILE_TYPE1A, type1a)
                                .attribute(null, METADATA_PROFILE_TYPE1B, type1b)
                                .attribute(null, METADATA_PROFILE_TYPE1C, type1c)
                                .attribute(null, METADATA_PROFILE_TYPE2, type2)
                                .attribute(null, METADATA_PROFILE_TYPE3, type3)
                                .endTag(null, METADATA_PROFILE_ITEM);
                    }
                    xmlSerializer.endTag(null, METADATA_PROFILE_DISABLED);
                }

                xmlSerializer.endTag(null, METADATA_PROFILE_OVERLAYS);
                xmlSerializer.endDocument();
                xmlSerializer.flush();
            }
        } catch (IOException ioe) {
            // Suppress exception
        }
    }

    public static HashMap<String, ProfileItem> readProfileState(String profileName,
                                                                int overlayState) {
        HashMap<String, ProfileItem> map = new HashMap<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            Node items = doc.getElementsByTagName(
                    overlayState == OverlayInfo.STATE_APPROVED_ENABLED ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED)
                    .item(0);

            if (items != null) {
                NodeList childNodes = items.getChildNodes();
                int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) childNodes.item(i);
                        ProfileItem item =
                                new ProfileItem(e.getAttribute(METADATA_PROFILE_PACKAGE_NAME));
                        item.setParentTheme(e.getAttribute(METADATA_PROFILE_PARENT));
                        item.setTargetPackage(e.getAttribute(METADATA_PROFILE_TARGET));
                        item.setType1a(e.getAttribute(METADATA_PROFILE_TYPE1A));
                        item.setType1b(e.getAttribute(METADATA_PROFILE_TYPE1B));
                        item.setType1c(e.getAttribute(METADATA_PROFILE_TYPE1C));
                        item.setType2(e.getAttribute(METADATA_PROFILE_TYPE2));
                        item.setType3(e.getAttribute(METADATA_PROFILE_TYPE3));
                        map.put(e.getAttribute(METADATA_PROFILE_PACKAGE_NAME), item);
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static List<String> readProfileStatePackage(String profileName, int overlayState) {
        List<String> list = new ArrayList<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            Node items = doc.getElementsByTagName(
                    overlayState == OverlayInfo.STATE_APPROVED_ENABLED ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED).item(0);

            if (items != null) {
                NodeList childNodes = items.getChildNodes();
                int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) childNodes.item(i);
                        list.add(element.getAttribute(METADATA_PROFILE_PACKAGE_NAME));
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<List<String>> readProfileStatePackageWithTargetPackage(String profileName,
                                                                              int overlayState) {
        List<List<String>> list = new ArrayList<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            Node items = doc.getElementsByTagName(
                    overlayState == OverlayInfo.STATE_APPROVED_ENABLED ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED).item(0);

            if (items != null) {
                NodeList childNodes = items.getChildNodes();
                int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) childNodes.item(i);
                        List<String> overlay = new ArrayList<>();
                        overlay.add(element.getAttribute(METADATA_PROFILE_PACKAGE_NAME));
                        overlay.add(element.getAttribute(METADATA_PROFILE_TARGET));
                        list.add(overlay);
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return list;
    }
}