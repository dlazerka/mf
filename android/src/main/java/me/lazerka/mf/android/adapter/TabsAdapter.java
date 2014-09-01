package me.lazerka.mf.android.adapter;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import me.lazerka.mf.android.activity.ContactsFragment;
import me.lazerka.mf.android.activity.map.MapFragment;

/**
 * @author Dzmitry Lazerka
 */
public class TabsAdapter extends FragmentPagerAdapter {
	private final ActionBar mActionBar;
	private final ViewPager mViewPager;

	public TabsAdapter(FragmentManager fragmentManager, ActionBar actionBar, ViewPager viewPager) {
		super(fragmentManager);
		mActionBar = actionBar;
		mViewPager = viewPager;
	}

	public void init() {
		OnPageChangeListener mPageChangeListener = new OnPageChangeListener();
		mViewPager.setOnPageChangeListener(mPageChangeListener);
		mViewPager.setAdapter(this);

		TabListener tabListener = new TabListener();

		Tab friendsTab = mActionBar.newTab().setText("Friends");
		friendsTab.setTabListener(tabListener);
		mActionBar.addTab(friendsTab);

		Tab mapTab = mActionBar.newTab().setText("Map");
		mapTab.setTabListener(tabListener);
		mActionBar.addTab(mapTab);

		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return 2;
	}

	@Override
	public Fragment getItem(int position) {
		switch (position) {
			case 0:
				return new ContactsFragment();
			case 1:
				return new MapFragment();
			default:
				throw new IllegalArgumentException("Unknown tab position=" + position);
		}
	}

	private class OnPageChangeListener implements ViewPager.OnPageChangeListener {
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}

	private class TabListener implements ActionBar.TabListener {
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			int position = tab.getPosition();
			mViewPager.setCurrentItem(position);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
