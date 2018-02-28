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

package projekt.substratum.common;

public enum Internal {
    ;

    // Options to unify the modes used in the application
    public static final String COMPILE_ENABLE = "CompileEnable";
    public static final String COMPILE_UPDATE = "CompileUpdate";
    public static final String DISABLE = "Disable";
    public static final String ENABLE = "Enable";
    public static final String ENABLE_MODE = "ENABLE";
    public static final String DISABLE_MODE = "DISABLE";
    public static final String ENABLE_DISABLE = "EnableDisable";
    public static final String MIX_AND_MATCH = "MixAndMatchMode";
    // Encrypted theme asset algorithm parameters
    public static final String ENCRYPTION_KEY_EXTRA = "encryption_key";
    public static final String IV_ENCRYPTION_KEY_EXTRA = "iv_encrypt_key";
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String SECRET_KEY_SPEC = "AES";
    // Byte access rate count for functions
    public static final int BYTE_ACCESS_RATE = 8192;
    // Refresher
    public static final String OVERLAY_REFRESH = "Overlay.REFRESH";
    public static final String AUTHENTICATE_RECEIVER = "projekt.substratum.AUTHENTICATE";
    public static final String AUTHENTICATED_RECEIVER = "projekt.substratum.PASS";
    public static final String ANDROMEDA_RECEIVER = "AndromedaReceiver.KILL";
    public static final String MAIN_ACTIVITY_RECEIVER = "MainActivity.KILL";
    public static final String THEME_FRAGMENT_REFRESH = "ThemeFragment.REFRESH";
    public static final String START_JOB_ACTION = ".START_JOB";
    public static final String JOB_COMPLETE = "job_complete";
    // IA Communication
    public static final String MIX_AND_MATCH_IA_TO_OVERLAYS = "newValue";
    public static final String SHEET_COMMAND = "command";
    // Showcase
    public static final String SHOWCASE_CACHE = "/ShowcaseCache/";
    // Boot Animation
    public static final String BOOTANIMATION_CACHE = "/BootAnimationCache/";
    public static final String BOOTANIMATION_CREATION_CACHE =
            "/BootAnimationCache/AnimationCreator/";
    public static final String BOOTANIMATION_PREVIEW_CACHE =
            "/BootAnimationCache/animation_preview/";
    // Fonts
    public static final String FONT_CACHE = "/FontCache/";
    public static final String FONT_PREVIEW_CACHE = "/FontCache/font_preview/";
    public static final String NORMAL_FONT = "Roboto-Regular.ttf";
    public static final String BOLD_FONT = "Roboto-Black.ttf";
    public static final String ITALICS_FONT = "Roboto-Italic.ttf";
    public static final String BOLD_ITALICS_FONT = "Roboto-BoldItalic.ttf";
    // Sounds
    public static final String SOUNDS_CACHE = "/SoundsCache/";
    public static final String SOUNDS_CREATION_CACHE = "/SoundsCache/SoundsInjector/";
    public static final String SOUNDS_PREVIEW_CACHE = "/SoundsCache/sounds_preview/";
    // Extraprocess Communication
    public static final String PACKAGE_INSTALL_URI = "application/vnd.android.package-archive";
    public static final String SUPPORTED_ROMS_FILE = "supported_roms.xml";
    public static final String MAIL_TYPE = "message/rfc822";
    // All the theme related variables, including those found in Fragments and Activities
    public static final String THEME_AUTHOR = "theme_author";
    public static final String THEME_NAME = "theme_name";
    public static final String THEME_PID = "theme_pid";
    public static final String THEME_PACKAGE = "package_name";
    public static final String THEME_HASH = "theme_hash";
    public static final String THEME_OMS = "oms_check";
    public static final String THEME_CERTIFIED = "certified";
    public static final String THEME_HASHPASSTHROUGH = "hash_passthrough";
    public static final String THEME_LAUNCH_TYPE = "theme_launch_type";
    public static final String THEME_DEBUG = "theme_debug";
    public static final String THEME_PIRACY_CHECK = "theme_piracy_check";
    public static final String THEME_LEGACY = "theme_legacy";
    public static final String THEME_WALLPAPER = "wallpaperUrl";
    public static final String THEME_CALLER = "calling_package_name";
    // Prefs
    public static final String SOUNDS_APPLIED = "sounds_applied";
    public static final String BOOT_ANIMATION = "bootanimation";
    public static final String SHUTDOWN_ANIMATION = "shutdownanimation";
    public static final String SHUTDOWNANIMATION_INTENT = "shutdownanimation";
    public static final String BOOT_ANIMATION_APPLIED = "bootanimation_applied";
    public static final String SHUTDOWN_ANIMATION_APPLIED = "shutdownanimation_applied";
    public static final String FONTS_APPLIED = "fonts_applied";
    // Permissions
    public static final int THEME_644 = 644;
    public static final int THEME_755 = 755;
    // Sounds
    public static final String ALARM = "alarm";
    public static final String NOTIFICATION = "notification";
    public static final String RINGTONE = "ringtone";
    public static final String EFFECT_TICK = "Effect_Tick";
    public static final String LOCK = "Lock";
    public static final String UNLOCK = "Unlock";
    // For Play store links
    public static final String PLAY_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
    // Misc Packages
    public static final String PHONE = "com.android.phone";
    public static final String PHONE_COMMON_FRAMEWORK = "com.android.phone.common";
    public static final String CONTACTS = "com.android.contacts";
    public static final String CONTACTS_COMMON_FRAMEWORK = "com.android.contacts.common";
    // Misc
    public static final String ENCRYPTED_FILE_EXTENSION = ".enc";
    public static final String HIDDEN_FOLDER = ".substratum";
    public static final String MAIN_FOLDER = "/substratum/";
    public static final String NO_MEDIA = "/substratum/.nomedia";
    public static final String PROFILE_DIRECTORY = "/substratum/profiles/";
    public static final String WALLPAPER_FILE_NAME = "/wallpaper.png";
    public static final String WALLPAPER_DIR = "/wallpaper";
    public static final String LOCK_WALL = "/wallpaper_lock";
    public static final String LOCK_WALLPAPER_FILE_NAME = "/wallpaper_lock.png";
    public static final String ALL_WALLPAPER = "all";
    public static final String HOME_WALLPAPER = "home";
    public static final String LOCK_WALLPAPER = "lock";
    public static final String CURRENT_WALLPAPERS = "current_wallpapers.xml";
    public static final String OVERLAYS_DIR = "overlays";
    public static final String OVERLAY_DIR = "overlay";
    public static final String OVERLAY_STATE_FILE = "overlays.xml";
    public static final String OVERLAY_PROFILE_STATE_FILE = "overlay_state.xml";
    public static final String SYSTEM_OVERLAY = "/system/overlay/";
    public static final String SYSTEM_VENDOR_OVERLAY = "/system/vendor/overlay/";
    public static final String PROFILE_AUDIO = "theme/audio";
    public static final String PROFILE_BOOTANIMATIONS = "theme/bootanimation.zip";
    public static final String PROFILE_FONTS = "theme/fonts";
    public static final String USERS_DIR = "/data/system/users/";
    public static final String THEME_DIR = "/theme";
    public static final String THEME_DIRECTORY = "/data/system/theme";
    public static final String AUDIO_THEME_DIRECTORY = "/data/system/theme/audio/";
    public static final String ALARM_THEME_DIRECTORY = "/data/system/theme/audio/alarms/";
    public static final String NOTIF_THEME_DIRECTORY = "/data/system/theme/audio/notifications/";
    public static final String RINGTONE_THEME_DIRECTORY = "/data/system/theme/audio/ringtones/";
    public static final String FONTS_THEME_DIRECTORY = "/data/system/theme/fonts/";
    public static final String BOOTANIMATION_DESCRIPTOR = "desc.txt";
    public static final String BOOTANIMATION = "bootanimation.zip";
    public static final String SHUTDOWNANIMATION = "shutdownanimation.zip";
    public static final String BOOTANIMATION_LOCATION = "/system/media/bootanimation.zip";
    public static final String BOOTANIMATION_BU = "bootanimation-backup.zip";
    public static final String BOOTANIMATION_BU_LOCATION = "/system/media/bootanimation-backup.zip";
    public static final String VALIDATOR_CACHE = "ValidatorCache";
    public static final String VALIDATOR_CACHE_DIR = "/ValidatorCache/";
    public static final String SYSTEM_ADDON_DIR = "/system/addon.d/";
    public static final String SUBSBOOT_ADDON = "/system/addon.d/81-subsboot.sh";
    public static final String XML_SERIALIZER =
            "http://xmlpull.org/v1/doc/features.html#indent-output";
    public static final String XML_UTF = "UTF-8";
    // Overlays.java
    public static final Integer SPECIAL_SNOWFLAKE_DELAY = 500;
    public static final Integer SPECIAL_SNOWFLAKE_DELAY_SS = 1500;
    public static final String XML_EXTENSION = ".xml";
    public static final String TYPE1A_PREFIX = "type1a_";
    public static final String TYPE1B_PREFIX = "type1b_";
    public static final String TYPE1C_PREFIX = "type1c_";
    public static final String TYPE2_PREFIX = "type2_";
    public static final String TYPE4_PREFIX = "type4_";
}