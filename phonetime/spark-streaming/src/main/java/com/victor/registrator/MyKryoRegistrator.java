package com.victor.registrator;

import com.esotericsoftware.kryo.Kryo;
import com.victor.common.model.SingleUserBehaviorRequestModel;
import com.victor.common.model.UserBehavorRequestModel;
import org.apache.spark.serializer.KryoRegistrator;

public class MyKryoRegistrator implements KryoRegistrator
{
  @Override
  public void registerClasses(Kryo kryo)
  {
    kryo.register(UserBehavorRequestModel.class);
    kryo.register(SingleUserBehaviorRequestModel.class);
  }
}
