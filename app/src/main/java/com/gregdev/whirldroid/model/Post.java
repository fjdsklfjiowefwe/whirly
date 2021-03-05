package com.gregdev.whirldroid.model;

public class Post {
	
	private String id;
	private User user;
	private String posted_time;
	private String content;
	private boolean edited;
	private boolean deleted;
	private boolean op;
	
	public Post(String id, User user, String posted_time, String content, boolean edited, boolean op) {
		this.id = id;
		this.user = user;
		this.posted_time = posted_time;
		this.content = content;
		this.edited = edited;
		this.op = op;
	}
	
	public String getId() {
		return id;
	}
	
	public User getUser() {
		return user;
	}
	
	public String getPostedTime() {
		return posted_time;
	}
	
	public String getContent() {
		return content;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isOp() {
		return op;
	}

	public void setOp(boolean op) {
		this.op = op;
	}
	
}
