package com.example.tsensors;

public class ListItem {
    String item;
    boolean isSelected = false;
    long time;
    public ListItem(String str) {
        super();
        item = str;
        time = parseDate(str);
    }
    public void setSelect(boolean b) {
        isSelected = b;
    }
    public boolean getSelect() {
        return isSelected;
    }
    public void setItem(String str) {
        item = str;
        time = parseDate(str);
    }
    public String getItem() {
        return item;
    }

    public long getTime() {
        return time;
    }
    protected long parseDate(String fileName) {
        String temp = fileName.substring(fileName.indexOf(utils.TEMPPATH) + utils.TEMPPATH.length());
        return Long.parseLong(temp);
    }
}
