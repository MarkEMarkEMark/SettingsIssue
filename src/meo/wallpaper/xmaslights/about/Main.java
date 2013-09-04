package meo.wallpaper.xmaslights.about;

import meo.wallpaper.xmaslights.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class Main extends Activity {
	final public int ABOUT = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, ABOUT, 0, "About");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ABOUT:
			AboutDialog about = new AboutDialog(this);
			about.setTitle("about this app");

			about.show();

			break;
		}
		return true;
	}
}
