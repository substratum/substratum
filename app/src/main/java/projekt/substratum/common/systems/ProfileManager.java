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
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import projekt.substratum.common.Packages;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;

import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayTarget;
import static projekt.substratum.common.References.metadataOverlayType1a;
import static projekt.substratum.common.References.metadataOverlayType1b;
import static projekt.substratum.common.References.metadataOverlayType1c;
import static projekt.substratum.common.References.metadataOverlayType2;
import static projekt.substratum.common.References.metadataOverlayType3;
import static projekt.substratum.common.References.metadataOverlayType4;
import static projekt.substratum.common.platform.ThemeManager.STATE_DISABLED;
import static projekt.substratum.common.platform.ThemeManager.STATE_ENABLED;

public enum ProfileManager {
    ;
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
    private static final String METADATA_PROFILE_TYPE4 = "type4";
    private static final String DAY = "day";

    public static void updateScheduledProfile(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context
                .ALARM_SERVICE);
        final Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        final PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        final PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String currentProfile = prefs.getString(SCHEDULED_PROFILE_CURRENT_PROFILE, "");
        final int dayHour = prefs.getInt(DAY_PROFILE_HOUR, 0);
        final int dayMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);
        final int nightHour = prefs.getInt(NIGHT_PROFILE_HOUR, 0);
        final int nightMinute = prefs.getInt(NIGHT_PROFILE_MINUTE, 0);

        // Set up current calendar instance
        final Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());

        // Set up day night calendar instance
        final Calendar calendarNight = Calendar.getInstance();
        calendarNight.setTimeInMillis(System.currentTimeMillis());
        calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
        calendarNight.set(Calendar.MINUTE, nightMinute);
        calendarNight.set(Calendar.SECOND, 0);

        final Calendar calendarDay = Calendar.getInstance();
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

    public static void enableScheduledProfile(final Context context, final String dayProfile,
                                              final int dayHour, final int dayMinute, final
                                              String nightProfile,
                                              final int nightHour, final int nightMinute) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        final AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context
                .ALARM_SERVICE);
        final Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        final PendingIntent nightIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        final PendingIntent dayIntent = PendingIntent.getBroadcast(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Set up current calendar instance
        final Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());

        // Set up day night calendar instance
        final Calendar calendarNight = Calendar.getInstance();
        calendarNight.setTimeInMillis(System.currentTimeMillis());
        calendarNight.set(Calendar.HOUR_OF_DAY, nightHour);
        calendarNight.set(Calendar.MINUTE, nightMinute);
        calendarNight.set(Calendar.SECOND, 0);

        final Calendar calendarDay = Calendar.getInstance();
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

    public static void disableScheduledProfile(final Context context) {
        final SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        final AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context
                .ALARM_SERVICE);
        final Intent intent = new Intent(context, ScheduledProfileReceiver.class);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, NIGHT);
        final PendingIntent nightIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, DAY);
        final PendingIntent dayIntent =
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

    @SuppressWarnings("RedundantCast")
    public static void writeProfileState(final Context context, final String profileName) {
        try {
            try (FileOutputStream outputStream = new FileOutputStream(
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + profileName + "/overlay_state.xml")) {
                final XmlSerializer xmlSerializer = Xml.newSerializer();
                xmlSerializer.setOutput(outputStream, "UTF-8");
                xmlSerializer.setFeature(
                        "http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializer.startDocument(null, true);
                xmlSerializer.startTag(null, METADATA_PROFILE_OVERLAYS);

                // Write enabled overlays
                final List<String> enabled = ThemeManager.listOverlays(context, STATE_ENABLED);
                if (!enabled.isEmpty()) {
                    xmlSerializer.startTag(null, METADATA_PROFILE_ENABLED);
                    for (final String packageName : enabled) {
                        final String target = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayTarget);
                        final String parent = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayParent);
                        final String type1a = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1a);
                        final String type1b = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1b);
                        final String type1c = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1c);
                        final String type2 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType2);
                        final String type3 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType3);
                        final String type4 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType4);

                        xmlSerializer.startTag(null, METADATA_PROFILE_ITEM)
                                .attribute(null, METADATA_PROFILE_PACKAGE_NAME,
                                        String.valueOf((Object) packageName))
                                .attribute(null, METADATA_PROFILE_TARGET,
                                        String.valueOf((Object) target))
                                .attribute(null, METADATA_PROFILE_PARENT,
                                        String.valueOf((Object) parent))
                                .attribute(null, METADATA_PROFILE_TYPE1A,
                                        String.valueOf((Object) type1a))
                                .attribute(null, METADATA_PROFILE_TYPE1B,
                                        String.valueOf((Object) type1b))
                                .attribute(null, METADATA_PROFILE_TYPE1C,
                                        String.valueOf((Object) type1c))
                                .attribute(null, METADATA_PROFILE_TYPE2,
                                        String.valueOf((Object) type2))
                                .attribute(null, METADATA_PROFILE_TYPE3,
                                        String.valueOf((Object) type3))
                                .attribute(null, METADATA_PROFILE_TYPE4,
                                        String.valueOf((Object) type4))
                                .endTag(null, METADATA_PROFILE_ITEM);
                    }
                    xmlSerializer.endTag(null, METADATA_PROFILE_ENABLED);
                }

                // Write disabled overlays
                final List<String> disabled = ThemeManager.listOverlays(context, STATE_DISABLED);
                if (!disabled.isEmpty()) {
                    xmlSerializer.startTag(null, METADATA_PROFILE_DISABLED);
                    for (final String packageName : disabled) {
                        final String target = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayTarget);
                        final String parent = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayParent);
                        final String type1a = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1a);
                        final String type1b = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1b);
                        final String type1c = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType1c);
                        final String type2 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType2);
                        final String type3 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType3);
                        final String type4 = Packages.getOverlayMetadata(
                                context, packageName, metadataOverlayType4);

                        xmlSerializer.startTag(null, METADATA_PROFILE_ITEM)
                                .attribute(null, METADATA_PROFILE_PACKAGE_NAME,
                                        String.valueOf((Object) packageName))
                                .attribute(null, METADATA_PROFILE_TARGET,
                                        String.valueOf((Object) target))
                                .attribute(null, METADATA_PROFILE_PARENT,
                                        String.valueOf((Object) parent))
                                .attribute(null, METADATA_PROFILE_TYPE1A,
                                        String.valueOf((Object) type1a))
                                .attribute(null, METADATA_PROFILE_TYPE1B,
                                        String.valueOf((Object) type1b))
                                .attribute(null, METADATA_PROFILE_TYPE1C,
                                        String.valueOf((Object) type1c))
                                .attribute(null, METADATA_PROFILE_TYPE2,
                                        String.valueOf((Object) type2))
                                .attribute(null, METADATA_PROFILE_TYPE3,
                                        String.valueOf((Object) type3))
                                .attribute(null, METADATA_PROFILE_TYPE4,
                                        String.valueOf((Object) type4))
                                .endTag(null, METADATA_PROFILE_ITEM);
                    }
                    xmlSerializer.endTag(null, METADATA_PROFILE_DISABLED);
                }

                xmlSerializer.endTag(null, METADATA_PROFILE_OVERLAYS);
                xmlSerializer.endDocument();
                xmlSerializer.flush();
            }
        } catch (final IOException ioe) {
            // Suppress exception
        }
    }

    public static Map<String, ProfileItem> readProfileState(final String profileName,
                                                            final int overlayState) {
        final Map<String, ProfileItem> map = new HashMap<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            final Node items = doc.getElementsByTagName(
                    (overlayState == STATE_ENABLED) ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED)
                    .item(0);

            if (items != null) {
                final NodeList childNodes = items.getChildNodes();
                final int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if ((int) childNodes.item(i).getNodeType() == (int) Node.ELEMENT_NODE) {
                        final Element e = (Element) childNodes.item(i);
                        final ProfileItem item =
                                new ProfileItem(e.getAttribute(METADATA_PROFILE_PACKAGE_NAME));
                        item.setParentTheme(e.getAttribute(METADATA_PROFILE_PARENT));
                        item.setTargetPackage(e.getAttribute(METADATA_PROFILE_TARGET));
                        item.setType1a(e.getAttribute(METADATA_PROFILE_TYPE1A));
                        item.setType1b(e.getAttribute(METADATA_PROFILE_TYPE1B));
                        item.setType1c(e.getAttribute(METADATA_PROFILE_TYPE1C));
                        item.setType2(e.getAttribute(METADATA_PROFILE_TYPE2));
                        item.setType3(e.getAttribute(METADATA_PROFILE_TYPE3));
                        item.setType4(e.getAttribute(METADATA_PROFILE_TYPE4));
                        map.put(e.getAttribute(METADATA_PROFILE_PACKAGE_NAME), item);
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static List<String> readProfileStatePackage(final String profileName, final int
            overlayState) {
        final List<String> list = new ArrayList<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            final Node items = doc.getElementsByTagName(
                    (overlayState == STATE_ENABLED) ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED).item(0);

            if (items != null) {
                final NodeList childNodes = items.getChildNodes();
                final int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if ((int) childNodes.item(i).getNodeType() == (int) Node.ELEMENT_NODE) {
                        final Element element = (Element) childNodes.item(i);
                        list.add(element.getAttribute(METADATA_PROFILE_PACKAGE_NAME));
                    }
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<List<String>> readProfileStatePackageWithTargetPackage(final String
                                                                                      profileName,
                                                                              final int
                                                                                      overlayState) {
        final List<List<String>> list = new ArrayList<>();
        try (InputStream input = new FileInputStream(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/substratum/profiles/" + profileName + "/overlay_state" +
                ".xml")) {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            final Node items = doc.getElementsByTagName(
                    (overlayState == STATE_ENABLED) ?
                            METADATA_PROFILE_ENABLED : METADATA_PROFILE_DISABLED).item(0);

            if (items != null) {
                final NodeList childNodes = items.getChildNodes();
                final int listLength = childNodes.getLength();
                for (int i = 0; i < listLength; i++) {
                    if ((int) childNodes.item(i).getNodeType() == (int) Node.ELEMENT_NODE) {
                        final Element element = (Element) childNodes.item(i);
                        final List<String> overlay = new ArrayList<>();
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