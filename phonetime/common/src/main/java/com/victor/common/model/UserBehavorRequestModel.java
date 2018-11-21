package com.victor.common.model;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class UserBehavorRequestModel extends BaseModel {

	private Long beginTime;

	private Long endTime;

	@JsonProperty("data")
	private List<SingleUserBehaviorRequestModel> singleUserBehaviorRequestModelList;

	private long userId;

	private String day;

	public Long getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Long beginTime) {
		this.beginTime = beginTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public List<SingleUserBehaviorRequestModel> getSingleUserBehaviorRequestModelList() {
		return singleUserBehaviorRequestModelList;
	}

	public void setSingleUserBehaviorRequestModelList(List<SingleUserBehaviorRequestModel> singleUserBehaviorRequestModelList) {
		this.singleUserBehaviorRequestModelList = singleUserBehaviorRequestModelList;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	@Override
	public String toString() {
		return "UserBehavorRequestModel{" +
				"beginTime=" + beginTime +
				", endTime=" + endTime +
				", singleUserBehaviorRequestModelList=" + singleUserBehaviorRequestModelList +
				", userId=" + userId +
				", day='" + day + '\'' +
				'}';
	}
}
