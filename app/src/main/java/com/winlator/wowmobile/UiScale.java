package com.winlator.wowmobile;

import android.content.Context;

import androidx.preference.PreferenceManager;

import com.winlator.core.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * Larger-than-1.0 interface scale for small phone screens.
 *
 * WoW 3.3.5's own UI Scale slider only goes from 0.64 to 1.0 (it can only shrink the UI). The
 * launcher installs a tiny addon that scales UIParent past 1.0 and seeds its saved value from the
 * "Interface size" setting in WoW Settings. In game, /uiscale 1.3 changes it too.
 */
final class UiScale {
    static final String PREF = "ui_scale";
    static final String[] LABELS = {"Normal (1.0)", "1.1x", "1.2x", "1.3x (recommended for phones)", "1.4x", "1.5x"};
    static final String[] VALUES = {"1.0", "1.1", "1.2", "1.3", "1.4", "1.5"};
    static final String DEFAULT = "1.3";
    private static final String ADDON = "WoWMobileUIScale";

    private static final String TOC =
        "## Interface: 30300\n" +
        "## Title: WoW Mobile UI Scale\n" +
        "## Notes: Scales the interface beyond the 1.0 limit of the built-in slider. /uiscale 1.3\n" +
        "## SavedVariables: WoWMobileUIScale\n" +
        "UIScale.lua\n";

    private static final String LUA =
        "WoWMobileUIScale = WoWMobileUIScale or 1.3\n" +
        "local pending = false\n" +
        "local function Apply()\n" +
        "  if InCombatLockdown() then pending = true return end\n" +
        "  pending = false\n" +
        "  local s = tonumber(WoWMobileUIScale) or 1\n" +
        "  if s < 0.5 then s = 0.5 elseif s > 2 then s = 2 end\n" +
        "  UIParent:SetScale(s)\n" +
        "end\n" +
        "local f = CreateFrame(\"Frame\")\n" +
        "f:RegisterEvent(\"PLAYER_LOGIN\")\n" +
        "f:RegisterEvent(\"PLAYER_ENTERING_WORLD\")\n" +
        "f:RegisterEvent(\"UI_SCALE_CHANGED\")\n" +
        "f:RegisterEvent(\"DISPLAY_SIZE_CHANGED\")\n" +
        "f:RegisterEvent(\"PLAYER_REGEN_ENABLED\")\n" +
        "f:SetScript(\"OnEvent\", function(self, event)\n" +
        "  if event ~= \"PLAYER_REGEN_ENABLED\" or pending then Apply() end\n" +
        "end)\n" +
        "SLASH_WOWMOBILEUISCALE1 = \"/uiscale\"\n" +
        "SlashCmdList[\"WOWMOBILEUISCALE\"] = function(msg)\n" +
        "  local v = tonumber(msg)\n" +
        "  if v then\n" +
        "    WoWMobileUIScale = v\n" +
        "    Apply()\n" +
        "    DEFAULT_CHAT_FRAME:AddMessage(\"UI scale set to \" .. v)\n" +
        "  else\n" +
        "    DEFAULT_CHAT_FRAME:AddMessage(\"UI scale is \" .. tostring(WoWMobileUIScale) .. \" - use /uiscale 1.3 (0.5 to 2)\")\n" +
        "  end\n" +
        "end\n";

    private UiScale() {}

    static String get(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF, DEFAULT);
    }

    static void set(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF, value).apply();
    }

    /** Installs/updates the addon files. Cheap and idempotent. */
    static void installAddon(GameFolder gameFolder) {
        File dir = new File(gameFolder.getAddOnsDir(), ADDON);
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        writeIfChanged(new File(dir, ADDON + ".toc"), TOC);
        writeIfChanged(new File(dir, "UIScale.lua"), LUA);
    }

    /** Seeds the account's saved scale so the launcher setting takes effect on next login. */
    static void applyToAccount(Context context, File accountDir) {
        File savedVariablesDir = new File(accountDir, "SavedVariables");
        if (!savedVariablesDir.isDirectory() && !savedVariablesDir.mkdirs()) return;
        String value;
        try {
            value = String.format(Locale.US, "%.2f", Float.parseFloat(get(context)));
        }
        catch (NumberFormatException e) {
            value = DEFAULT;
        }
        writeIfChanged(new File(savedVariablesDir, ADDON + ".lua"), "WoWMobileUIScale = " + value + "\n");
    }

    private static void writeIfChanged(File file, String content) {
        if (file.isFile() && content.equals(FileUtils.readString(file))) return;
        FileUtils.writeString(file, content);
    }
}
