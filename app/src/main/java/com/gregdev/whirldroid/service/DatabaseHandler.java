package com.gregdev.whirldroid.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.gregdev.whirldroid.model.Forum;

public class DatabaseHandler extends SQLiteOpenHelper {
    
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Whirldroid";
    
    /** Tables **/
    private static final String TBL_FAVOURITE_FORUMS = "favourite_forums";
    
    /** Favourite Forum Table Columns **/
    private static final String FORUM_ID = "forum_id";
    private static final String FORUM_NAME = "forum_name";
    
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FF_TABLE = " CREATE TABLE " + TBL_FAVOURITE_FORUMS + "("
                + FORUM_ID + " INTEGER PRIMARY KEY, " + FORUM_NAME + " TEXT)";
        db.execSQL(CREATE_FF_TABLE);                                                                                                     
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    }
    
    public void addFavouriteForum(Forum forum) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(FORUM_ID, forum.getId());
        values.put(FORUM_NAME, forum.getTitle());
        
        db.insert(TBL_FAVOURITE_FORUMS, null, values);
        db.close();
    }
    
    public void removeFavouriteForum(Forum forum) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TBL_FAVOURITE_FORUMS, FORUM_ID + " = ?", new String[] { String.valueOf(forum.getId()) });
        db.close();
    }
    
    public List<Forum> getFavouriteForums() {
        ArrayList<Forum> forums = new ArrayList<Forum>();
        
        String query = "SELECT * FROM " + TBL_FAVOURITE_FORUMS;
        
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                forums.add(new Forum(cursor.getInt(0), cursor.getString(1), "Favourites"));
            }
            while (cursor.moveToNext());
        }
        
        db.close();
        
        return forums;
    }
    
    public Boolean isInFavourites(Forum forum) {
        for (Forum f : getFavouriteForums()) {
            if (f.getId() == forum.getId()) {
                return true;
            }
        }

        return false;
    }

}
