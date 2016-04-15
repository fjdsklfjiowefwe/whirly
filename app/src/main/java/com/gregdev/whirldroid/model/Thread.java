package com.gregdev.whirldroid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import com.gregdev.whirldroid.Whirldroid;

public class Thread implements Comparable<Thread>, Serializable {
    private static final long serialVersionUID = 6143751282281390955L;
    private int    id;
    private String title;
    private Date   last_date;
    private String last_poster;
    private String last_poster_id = "";
    private String forum;
    private int    forum_id;
    private String notebar;
    
    private boolean deleted = false;
    private boolean closed  = false;
    private boolean sticky  = false;
    private boolean moved   = false;

    // for watched threads
    private int unread    = 0;
    private int last_page = 0;
    private int last_post = 0;
    
    // for popular/scraped threads
    private String last_date_text;
    private String original_poster;
    private ArrayList<Post> posts;
    private int page_count;
    
    
    public Thread(int id, String title, Date last_date, String last_poster, String forum, int forum_id) {
        this.id          = id;
        this.title       = Whirldroid.removeCommonHtmlChars(title);
        this.last_date   = last_date;
        this.last_poster = last_poster;
        this.forum       = forum;
        this.forum_id    = forum_id;
    }
    
    public Thread(int id, String title, Date last_date, String last_poster, String forum, int forum_id, int unread, int last_page, int last_post) {
        this(id, title, last_date, last_poster, forum, forum_id);
        this.unread = unread;
        this.last_page = last_page;
        this.last_post = last_post;
    }
    
    public int getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public Date getLastDate() {
        return last_date;
    }
    
    public String getLastPoster() {
        return last_poster;
    }
    
    public String getLastPosterId() {
        return last_poster_id;
    }
    
    public void setLastPosterId(String last_poster_id) {
        this.last_poster_id = last_poster_id;
    }
    
    public String getForum() {
        return forum;
    }
    
    public int getForumId() {
        return forum_id;
    }

    public void setForumId(int forum_id) {
        this.forum_id = forum_id;
    }
    
    public String getNotebar() {
        return notebar;
    }

    public void setNotebar(String notebar) {
        this.notebar = notebar;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }
    
    public boolean isMoved() {
        return moved;
    }
    
    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }
    
    public boolean hasUnreadPosts() {
        return unread != 0;
    }
    
    public int getLastPage() {
        return last_page;
    }
    
    public int getLastPost() {
        return last_post;
    }
    
    public String getLastDateText() {
        return last_date_text;
    }
    
    public void setLastDateText(String last_date_text) {
        this.last_date_text = last_date_text;
    }
    
    public String getOriginalPoster() {
        return original_poster;
    }
    
    public void setOriginalPoster(String original_poster) {
        this.original_poster = original_poster;
    }
    
    public ArrayList<Post> getPosts() {
        return posts;
    }
    
    public void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }
    
    public int getPageCount() {
        return page_count;
    }
    
    public void setPageCount(int page_count) {
        this.page_count = page_count;
    }
    
    @Override
    public String toString() {
        return title;
    }
    
    public int compareTo(Thread t) {
        if (last_date == null || t.getLastDate() == null) return 0;
        return t.getLastDate().compareTo(last_date);
    }
}
