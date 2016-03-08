package com.gregdev.whirldroid.models;

import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

import com.gregdev.whirldroid.Whirldroid;

public class User implements Parcelable {
	private String id;
	private String name;
	private String group;
	private String quip;
	private Map<String, String> info;
	
	public User(String id, String name) {
		this.id   = id;
		this.name = name;
		this.info = null;
	}
	
	private User(Parcel in) {
		id    = in.readString();
		name  = in.readString();
		group = in.readString();
		quip  = in.readString();
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return the quip
	 */
	public String getQuip() {
		return quip;
	}

	/**
	 * @param quip the quip to set
	 */
	public void setQuip(String quip) {
		this.quip = quip;
	}
	
	public void downloadInfo() {
		info = Whirldroid.getApi().scrapeUserInfo(id);
	}
	
	public Map<String, String> getInfo() {
		return info;
	}

	public static final Creator<User> CREATOR = new Creator<User>() {
		public User createFromParcel(Parcel in) {
		    return new User(in);
		}
		
		public User[] newArray(int size) {
		    return new User[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(name);
		out.writeString(group);
		out.writeString(quip);
	}
}
