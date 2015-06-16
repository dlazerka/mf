package me.lazerka.mf.android.adapter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Dzmitry Lazerka
 */
public class FriendInfo {
	public long id;
	public String lookupKey;
	public String displayName;
	public String photoUri;
	public final Set<String> emails = new HashSet<>();
}
