package me.lazerka.mf.android.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.adapter.TabsAdapter;

/**
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setTitle("MF");

		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

		final ActionBar actionBar = getActionBar();
		assert actionBar != null;
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		TabsAdapter tabsAdapter = new TabsAdapter(getFragmentManager(), actionBar, viewPager);
		tabsAdapter.init();

		if (savedInstanceState != null) {
			actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 1));
		}
		// Watch for button clicks.
		Button button = (Button) findViewById(R.id.send_my);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "Work In Progress", Toast.LENGTH_SHORT).show();
			}
		});
	}
}
