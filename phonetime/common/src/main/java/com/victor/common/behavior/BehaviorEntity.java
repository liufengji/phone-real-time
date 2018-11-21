package com.victor.common.behavior;

public class BehaviorEntity {

    private String userId;
    private String day;
    private String beginTime;
    private String endTime;

    private Behavior[] data;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Behavior[] getData() {
        return data;
    }

    public void setData(Behavior[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "BehaviorEntity{" +
                "userId='" + userId + '\'' +
                ", day='" + day + '\'' +
                ", beginTime='" + beginTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", data[0].Activetime =" + data[0].getActivetime() +
                ", data[0].PackageName =" + data[0].getPackageName() +
                ", data[1].Activetime =" + data[1].getActivetime() +
                ", data[1].PackageName =" + data[1].getPackageName() +
                '}';
    }
}
