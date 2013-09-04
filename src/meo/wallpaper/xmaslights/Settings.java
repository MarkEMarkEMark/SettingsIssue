package meo.wallpaper.xmaslights;

import rajawali.wallpaper.Wallpaper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

// Deprecated PreferenceActivity methods are used for API Level 10 (and lower) compatibility
// https://developer.android.com/guide/topics/ui/settings.html#Overview
@SuppressWarnings("deprecation")
public class Settings extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String KEY_MODE = "meoprefMode";
	public static final String KEY_PIXEL_LAYOUT = "meoprefPixelLayout";
	public static final String KEY_STARBURST = "meoprefStarburst";
	public static final String KEY_DISPLAYTIME = "meoprefDisplaytime";
	public static final String KEY_FADETIME = "meoprefFadetime";
	public static final String KEY_LAYOUTTIME = "meoprefLayouttime";
	public static final String KEY_PATTERNS = "meoprefPatterns";
	public static final String KEY_CROSSFADES = "meoprefCrossfades";	
	public static final String KEY_DEVELOPER = "meoprefDev";
	public static final String KEY_BULB_SIZE = "meoprefSize";
	public static final String KEY_ROTATE = "meoprefRotate";
	
	//final public int ABOUT = 0;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(Wallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.settings);
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void onDestroy() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		//if (key.equals(KEY_PIXELLAYOUT)) {
		//	Preference connectionPref = findPreference(key);
			// Set summary to be the user-description for the selected value
		//	connectionPref.setSummary(sharedPreferences.getString(key, ""));
		//}
	}
}