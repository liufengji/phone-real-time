package com.victor.common.behavior;

public class Behavior {

    private String packageName;
    private String activetime;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActivetime() {
        return activetime;
    }

    public void setActivetime(String activetime) {
        this.activetime = activetime;
    }

    @Override
    public String toString() {
        return "Behavior{" +
                "packageName='" + packageName + '\'' +
                ", activetime='" + activetime + '\'' +
                '}';
    }
}
