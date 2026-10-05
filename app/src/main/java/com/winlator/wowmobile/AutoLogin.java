package com.winlator.wowmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import androidx.preference.PreferenceManager;

import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional auto-login for the WoW client.
 *
 * Before launch the account name is written to Config.wtf (WoW's "remembered account"), so the
 * login screen opens with the password box focused. Once the WoW window appears we wait for the
 * login screen to load, type the password and press Enter, and can optionally press Enter again
 * at character select to enter the world with the last-played character.
 */
public final class AutoLogin {
    public static final String PREF_ENABLED = "autologin_enabled";
    public static final String PREF_ACCOUNT = "autologin_account";
    public static final String PREF_PASSWORD = "autologin_password";
    public static final String PREF_DELAY = "autologin_delay";
    public static final String PREF_ENTER_WORLD = "autologin_enter_world";
    public static final int DEFAULT_DELAY_SECONDS = 10;
    private static final int ENTER_WORLD_DELAY_SECONDS = 6;
    private static final long KEY_INTERVAL_MS = 40;

    private AutoLogin() {}

    public static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isEnabled(Context context) {
        SharedPreferences p = prefs(context);
        return p.getBoolean(PREF_ENABLED, false) && !p.getString(PREF_ACCOUNT, "").isEmpty();
    }

    /** Called before launching: makes WoW pre-fill the account name and focus the password box. */
    public static void prepareConfig(Context context, Provisioner provisioner) {
        if (!isEnabled(context)) return;
        provisioner.setConfigValue("accountName", prefs(context).getString(PREF_ACCOUNT, "").trim());
    }

    /** Watches for the WoW window and types the login once it has had time to load. */
    public static void attach(Context context, XServer xServer) {
        if (!isEnabled(context)) return;
        SharedPreferences p = prefs(context);
        final String password = p.getString(PREF_PASSWORD, "");
        final int delaySeconds = Math.max(3, parseInt(p.getString(PREF_DELAY, ""), DEFAULT_DELAY_SECONDS));
        final boolean enterWorld = p.getBoolean(PREF_ENTER_WORLD, false);
        if (password.isEmpty()) return;

        final AtomicBoolean started = new AtomicBoolean(false);
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onMapWindow(Window window) {
                maybeStart(window);
            }

            @Override
            public void onModifyWindowProperty(Window window, com.winlator.xserver.Property property) {
                // The title can arrive after the window is mapped.
                maybeStart(window);
            }

            private void maybeStart(Window window) {
                if (!isWowWindow(window) || !started.compareAndSet(false, true)) return;
                Thread thread = new Thread(() -> {
                    sleep(delaySeconds * 1000L);
                    typeText(xServer, password);
                    pressEnter(xServer);
                    if (enterWorld) {
                        sleep(ENTER_WORLD_DELAY_SECONDS * 1000L);
                        pressEnter(xServer);
                    }
                }, "WoWAutoLogin");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private static boolean isWowWindow(Window window) {
        String name = window.getName();
        return window.attributes.isMapped() && name != null && name.toLowerCase().contains("world of warcraft");
    }

    private static void typeText(XServer xServer, String text) {
        KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        for (char c : text.toCharArray()) {
            KeyEvent[] events = map.getEvents(new char[]{c});
            if (events != null) {
                for (KeyEvent event : events) {
                    xServer.keyboard.onKeyEvent(event);
                    sleep(KEY_INTERVAL_MS);
                }
            }
            else {
                // Characters with no key on the virtual keyboard (e.g. accented letters).
                xServer.keyboard.onKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), String.valueOf(c), KeyCharacterMap.VIRTUAL_KEYBOARD, 0));
                sleep(KEY_INTERVAL_MS * 2);
            }
        }
    }

    private static void pressEnter(XServer xServer) {
        long now = SystemClock.uptimeMillis();
        xServer.keyboard.onKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0));
        sleep(KEY_INTERVAL_MS);
        xServer.keyboard.onKeyEvent(new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0));
        sleep(KEY_INTERVAL_MS);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
