package com.winlator.wowmobile;

import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Edits WoW's own configuration (Config.wtf + realmlist.wtf) while the game is off.
 */
public class WowSettingsActivity extends AppCompatActivity {
    private static final String[] RESOLUTIONS = {"800x360", "960x432", "1200x540", "1600x720"};
    private static final String[] FARCLIP_LABELS = {"Near (fastest)", "Medium", "Far (slower)"};
    private static final String[] FARCLIP_VALUES = {"400", "727", "1000"};
    static final String[] MAXFPS_LABELS = {"30 fps (recommended)", "40 fps", "45 fps", "60 fps", "Unlimited"};
    static final String[] MAXFPS_VALUES = {"30", "40", "45", "60", "0"};
    static final String DEFAULT_MAXFPS = "30";

    private GameFolder gameFolder;
    private Provisioner provisioner;
    private Spinner sRealmlist;
    private EditText etCustomRealmlist;
    private Spinner sResolution;
    private Spinner sFarclip;
    private Spinner sMaxFps;
    private Spinner sUiScale;
    private ArrayList<String> realmlistItems;
    private CheckBox cbAutoLogin;
    private EditText etAutoLoginAccount;
    private EditText etAutoLoginPassword;
    private EditText etAutoLoginDelay;
    private CheckBox cbAutoLoginEnterWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppUtils.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wow_settings_activity);
        setSupportActionBar(findViewById(R.id.Toolbar));
        getSupportActionBar().setTitle(R.string.wow_settings);

        String path = PreferenceManager.getDefaultSharedPreferences(this).getString(WowMobileActivity.PREF_GAME_FOLDER, null);
        if (path == null || !(gameFolder = new GameFolder(path)).isValid()) {
            AppUtils.showToast(this, R.string.wow_status_select_folder);
            finish();
            return;
        }
        provisioner = new Provisioner(this, gameFolder);

        sRealmlist = findViewById(R.id.SRealmlist);
        etCustomRealmlist = findViewById(R.id.ETCustomRealmlist);
        sResolution = findViewById(R.id.SResolution);
        sFarclip = findViewById(R.id.SFarclip);
        sMaxFps = findViewById(R.id.SMaxFps);
        sUiScale = findViewById(R.id.SUiScale);

        TextView tvLocale = findViewById(R.id.TVLocale);
        String locale = gameFolder.getLocale();
        tvLocale.setText(getString(R.string.wow_detected_locale)+": "+(locale != null ? locale : "?"));

        loadRealmlists();
        loadGraphics();
        loadAutoLogin();

        findViewById(R.id.BTSave).setOnClickListener((v) -> save());
    }

    /** Active realm + any commented alternatives kept in realmlist.wtf + our default. */
    private void loadRealmlists() {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        String active = provisioner.getActiveRealmlist();
        items.add(active);

        File file = gameFolder.getRealmlistFile();
        if (file != null && file.isFile()) {
            for (String line : FileUtils.readString(file).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    String uncommented = trimmed.replaceFirst("^#+ *", "");
                    if (uncommented.toLowerCase().startsWith("set realmlist")) {
                        String host = uncommented.substring("set realmlist".length()).trim();
                        if (!host.isEmpty()) items.add(host);
                    }
                }
            }
        }
        items.add(Provisioner.DEFAULT_REALMLIST);

        realmlistItems = new ArrayList<>(items);
        realmlistItems.add(getString(R.string.wow_custom_realmlist));
        sRealmlist.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, realmlistItems));
        sRealmlist.setSelection(realmlistItems.indexOf(active));

        sRealmlist.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean custom = position == realmlistItems.size()-1;
                etCustomRealmlist.setVisibility(custom ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        etCustomRealmlist.setVisibility(View.GONE);
    }

    private void loadGraphics() {
        sResolution.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, RESOLUTIONS));
        String resolution = provisioner.getConfigValue("gxResolution");
        int index = 1;
        for (int i = 0; i < RESOLUTIONS.length; i++) if (RESOLUTIONS[i].equals(resolution)) index = i;
        sResolution.setSelection(index);

        sFarclip.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FARCLIP_LABELS));
        String farclip = provisioner.getConfigValue("farclip");
        int farclipIndex = 1;
        for (int i = 0; i < FARCLIP_VALUES.length; i++) if (FARCLIP_VALUES[i].equals(farclip)) farclipIndex = i;
        sFarclip.setSelection(farclipIndex);

        sMaxFps.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MAXFPS_LABELS));
        String maxFps = provisioner.getConfigValue("maxFPS");
        if (maxFps == null) maxFps = DEFAULT_MAXFPS;
        int fpsIndex = 0;
        for (int i = 0; i < MAXFPS_VALUES.length; i++) if (MAXFPS_VALUES[i].equals(maxFps)) fpsIndex = i;
        sMaxFps.setSelection(fpsIndex);

        sUiScale.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, UiScale.LABELS));
        String uiScale = UiScale.get(this);
        int uiIndex = 3;
        for (int i = 0; i < UiScale.VALUES.length; i++) if (UiScale.VALUES[i].equals(uiScale)) uiIndex = i;
        sUiScale.setSelection(uiIndex);
    }

    private void loadAutoLogin() {
        cbAutoLogin = findViewById(R.id.CBAutoLogin);
        etAutoLoginAccount = findViewById(R.id.ETAutoLoginAccount);
        etAutoLoginPassword = findViewById(R.id.ETAutoLoginPassword);
        etAutoLoginDelay = findViewById(R.id.ETAutoLoginDelay);
        cbAutoLoginEnterWorld = findViewById(R.id.CBAutoLoginEnterWorld);

        SharedPreferences prefs = AutoLogin.prefs(this);
        cbAutoLogin.setChecked(prefs.getBoolean(AutoLogin.PREF_ENABLED, false));
        etAutoLoginAccount.setText(prefs.getString(AutoLogin.PREF_ACCOUNT, ""));
        etAutoLoginPassword.setText(prefs.getString(AutoLogin.PREF_PASSWORD, ""));
        etAutoLoginDelay.setText(prefs.getString(AutoLogin.PREF_DELAY, String.valueOf(AutoLogin.DEFAULT_DELAY_SECONDS)));
        cbAutoLoginEnterWorld.setChecked(prefs.getBoolean(AutoLogin.PREF_ENTER_WORLD, false));
    }

    private void saveAutoLogin() {
        String account = etAutoLoginAccount.getText().toString().trim();
        AutoLogin.prefs(this).edit()
            .putBoolean(AutoLogin.PREF_ENABLED, cbAutoLogin.isChecked())
            .putString(AutoLogin.PREF_ACCOUNT, account)
            .putString(AutoLogin.PREF_PASSWORD, etAutoLoginPassword.getText().toString())
            .putString(AutoLogin.PREF_DELAY, etAutoLoginDelay.getText().toString().trim())
            .putBoolean(AutoLogin.PREF_ENTER_WORLD, cbAutoLoginEnterWorld.isChecked())
            .apply();
        // Pre-fill the account name now too, so the login screen focuses the password box.
        if (cbAutoLogin.isChecked() && !account.isEmpty()) provisioner.setConfigValue("accountName", account);
    }

    private void save() {
        String host;
        if (sRealmlist.getSelectedItemPosition() == realmlistItems.size()-1) {
            host = etCustomRealmlist.getText().toString().trim();
            if (host.isEmpty()) {
                AppUtils.showToast(this, R.string.wow_custom_realmlist);
                return;
            }
        }
        else host = realmlistItems.get(sRealmlist.getSelectedItemPosition());

        provisioner.ensureRealmlist(host);
        provisioner.setConfigValue("gxResolution", RESOLUTIONS[sResolution.getSelectedItemPosition()]);
        provisioner.setConfigValue("farclip", FARCLIP_VALUES[sFarclip.getSelectedItemPosition()]);
        String fps = MAXFPS_VALUES[sMaxFps.getSelectedItemPosition()];
        provisioner.setConfigValue("maxFPS", fps);
        provisioner.setConfigValue("maxFPSBk", fps);
        UiScale.set(this, UiScale.VALUES[sUiScale.getSelectedItemPosition()]);
        for (File accountDir : gameFolder.getAccountDirs()) UiScale.applyToAccount(this, accountDir);
        saveAutoLogin();

        AppUtils.showToast(this, R.string.wow_settings_saved);
        finish();
    }
}
