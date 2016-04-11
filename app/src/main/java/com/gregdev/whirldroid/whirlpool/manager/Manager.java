package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public abstract class Manager<T> {

    protected ArrayList<T> items;
    protected String cacheFileName;
    protected long lastUpdated = 0;
    protected int maxAge = 0;

    public ArrayList<T> getItems() {
        if (items == null || items.isEmpty()) { // no data in memory, get from cache file
            items = readFromCacheFile(cacheFileName);
        }

        return items;
    }

    public void setItems(ArrayList<T> items) {
        lastUpdated = System.currentTimeMillis() / 1000;
        writeToCacheFile(cacheFileName, items);
        this.items = items;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public abstract void download() throws WhirlpoolApiException;

    public boolean needToDownload() {
        long cache_file_age = getCacheFileAge(cacheFileName);
        if (cache_file_age != -1) { // cache file exists
            if (cache_file_age < 60 * maxAge) { // file not too old
                return false;
            }
        }

        if (items != null && items.size() > 0) { // data in memory
            if ((System.currentTimeMillis() / 1000) - lastUpdated < 60 * maxAge) { // memory data not too old
                return false;
            }
        }

        // if we get here, then we have don't useable cache data
        return true;
    }

    /**
     * Returns the age of the cache file in milliseconds
     * @return file age in seconds
     */
    protected long getCacheFileAge(String cacheFileName) {
        File cacheFile = Whirldroid.getContext().getFileStreamPath(cacheFileName);
        long now = System.currentTimeMillis();
        long fileLastModified = cacheFile.lastModified();

        if (fileLastModified == 0) return -1; // cache file doesn't exist

        long diff = (now - fileLastModified); // diff in milliseconds

        return diff / 1000;
    }

    protected void writeToCacheFile(String cache_file, ArrayList<T> data) {
        try {
            FileOutputStream fos = Whirldroid.getContext().openFileOutput(cache_file, 0x0000);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(data);
            out.close();

        } catch (IOException e) {
            // error writing cache, meh
        }
    }

    protected ArrayList<T> readFromCacheFile(String cache_file) {
        ArrayList<T> data = null;

        try {
            FileInputStream fis = Whirldroid.getContext().openFileInput(cache_file);
            ObjectInputStream in = new ObjectInputStream(fis);
            data = (ArrayList<T>) in.readObject();
            in.close();

        } catch (Exception e) { }

        return data;
    }

}