package com.victor.common.model;

import org.codehaus.jackson.annotate.JsonProperty;

public class SingleUserBehaviorRequestModel extends BaseModel {

  private static final long serialVersionUID = 1L;

  private String packageName;

  @JsonProperty("activetime")
  private long activeTime;

  public String getPackageName()
  {
    return packageName;
  }

  public void setPackageName(String packageName)
  {
    this.packageName = packageName;
  }

  public long getActiveTime()
  {
    return activeTime;
  }

  public void setActiveTime(long activeTime)
  {
    this.activeTime = activeTime;
  }

  @Override
  public String toString() {
    return "SingleUserBehaviorRequestModel{" +
            "packageName='" + packageName + '\'' +
            ", activeTime=" + activeTime +
            '}';
  }
}
