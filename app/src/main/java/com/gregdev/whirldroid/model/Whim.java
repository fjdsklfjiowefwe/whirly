package com.gregdev.whirldroid.model;

import java.io.Serializable;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.gregdev.whirldroid.Whirldroid;

public class Whim implements Parcelable, Serializable {
	private static final long serialVersionUID = -8647082511775037802L;
	private int id;
	private int from_id;
	private String from_name;
	private int viewed;
	private int replied;
	private Date date;
	private String content;

	public Whim(int id, int from_id, String from_name, int viewed, int replied, Date date, String content) {
		this.id        = id;
		this.from_id   = from_id;
		this.from_name = from_name;
		this.viewed    = viewed;
		this.replied   = replied;
		this.date      = date;
		this.content   = Whirldroid.removeCommonHtmlChars(content);
	}
	
	private Whim(Parcel in) {
		id        = in.readInt();
		from_id   = in.readInt();
		from_name = in.readString();
		viewed    = in.readInt();
		replied   = in.readInt();
		date      = (Date) in.readValue(null);
		content   = in.readString();
	}
	
	public int getId() {
		return id;
	}

	public String getContent() {
		return content;
	}
	
	public String getFromName() {
		return from_name;
	}
	
	public Date getDate() {
		return date;
	}
	
	public boolean isRead() {
		if (viewed == 1) {
			return true;
		}
		return false;
	}
	
	public void setRead(boolean read) {
		if (read) {
			viewed = 1;
		}
		else {
			viewed = 0;
		}
	}

	@Override
	public String toString() {
		if (content.length() > 20) {
			return content.substring(0, 20).replace("\r\n", " ") + "...";
		}
		return content;
	}
	
	public static final Creator<Whim> CREATOR = new Creator<Whim>() {
		public Whim createFromParcel(Parcel in) {
		    return new Whim(in);
		}
		
		public Whim[] newArray(int size) {
		    return new Whim[size];
		}
	};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(id);
		out.writeInt(from_id);
		out.writeString(from_name);
		out.writeInt(viewed);
		out.writeInt(replied);
		out.writeValue(date);
		out.writeString(content);
	}
}