package client;

import com.alibaba.fastjson.JSONObject;
import common.Behavior;
import common.BehaviorEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class GenBeahavior {

    public static void main(String[] args) {
        gather();
    }

    //组合数据->转成json->发送
    public static void gather() {

        Behavior[] behaviorsArr = new Behavior[2];

        try {
            for (int i = 1; i <= 200000000; i++) {

                BehaviorEntity behaviorEntity = new BehaviorEntity();

                String s = UUID.randomUUID().toString();
                // UID生成 生成随机数
                Random randomUid = new Random();
                String uid = String.valueOf(randomUid.nextInt(100));
                behaviorEntity.setUserId(uid);

                // BeginTime生成
                Calendar beginCal = randomDateBetweenMinAndMax();
                String beginTime = String.valueOf(beginCal.getTimeInMillis());
                behaviorEntity.setBeginTime(beginTime);
                // System.out.println(beginTime);

                // EndTime生成
                Random randomTimeMid = new Random();
                int timeMid = randomTimeMid.nextInt(1800000);
                String endTime = String.valueOf(Long.valueOf(beginTime) + timeMid);
                behaviorEntity.setEndTime(endTime);

                // 日期生成
                Calendar calDate = beginCal;
                calDate.set(Calendar.HOUR_OF_DAY, 0);
                calDate.set(Calendar.MINUTE, 0);
                calDate.set(Calendar.SECOND, 0);
                Date dayDate = calDate.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String day = sdf.format(dayDate);
                behaviorEntity.setDay(day);
                System.out.println(day);

                for (i = 0; i < 2; i++) {
                    Behavior behavior = new Behavior();

                    String appId = String.valueOf(randomUid.nextInt(100));
                    String appName = "App" + appId;
                    behavior.setPackageName(appName);

                    // ActiveTime控制在15分钟内
                    String activeTime = String.valueOf(randomUid.nextInt(900000));
                    behavior.setActivetime(activeTime);

                    behaviorsArr[i] = behavior;
                }

                behaviorEntity.setData(behaviorsArr);

                String json = JSONObject.toJSONString(behaviorEntity);
                //System.out.println(json);

                UploadUtil.upload(json);

                Thread.sleep(3000);

            }
        }catch (Exception ex) {
            System.out.println(ex);
        }
    }

    //时间
    public static Calendar randomDateBetweenMinAndMax() {

        Calendar calendar = Calendar.getInstance();
        //注意月份要减去1
        calendar.set(2017, 11, 1);
        calendar.getTime().getTime();
        //根据需求，这里要将时分秒设置为0
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long min = calendar.getTime().getTime();

        calendar.set(2017, 11, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.getTime().getTime();
        long max = calendar.getTime().getTime();

        //得到大于等于min小于max的double值
        double randomDate = Math.random() * (max - min) + min;

        //将double值舍入为整数，转化成long类型
        calendar.setTimeInMillis(Math.round(randomDate));

        return calendar;
    }
}
