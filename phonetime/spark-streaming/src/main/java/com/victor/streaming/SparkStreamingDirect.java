package com.victor.streaming;

import com.victor.key.UserHourPackageKey;
import com.victor.common.model.SingleUserBehaviorRequestModel;
import com.victor.common.model.UserBehaviorStatModel;
import com.victor.common.model.UserBehavorRequestModel;
import com.victor.service.BehaviorStatService;
import com.victor.common.utils.DateUtils;
import com.victor.common.utils.JSONUtil;
import com.victor.common.utils.MyStringUtil;
import com.victor.common.utils.PropertiesUtil;
import kafka.common.TopicAndPartition;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.HasOffsetRanges;
import org.apache.spark.streaming.kafka.KafkaCluster;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.apache.spark.streaming.kafka.OffsetRange;
import scala.Predef;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class SparkStreamingDirect {

    public static Logger log = Logger.getLogger(SparkStreamingDirect.class);

    public static void main(String[] args) throws Exception {

        final Properties serverProps = PropertiesUtil.getProperties("spark-streaming/src/main/resources/config.properties");
        //获取checkpoint的hdfs路径
        String checkpointPath = serverProps.getProperty("streaming.checkpoint.path");

        //如果checkpointPath hdfs目录下的有文件，则反序列化文件生产context,否则使用函数createContext返回的context对象
        JavaStreamingContext javaStreamingContext = JavaStreamingContext.getOrCreate(checkpointPath, createContext(serverProps));
        javaStreamingContext.start();

        javaStreamingContext.awaitTermination();
    }

    /**
     * 根据配置文件以及业务逻辑创建JavaStreamingContext 创建spark streaming
     *
     * @param serverProps
     * @return
     */
    public static Function0<JavaStreamingContext> createContext(final Properties serverProps) {

        Function0<JavaStreamingContext> createContextFunc = new Function0<JavaStreamingContext>() {
            @Override
            public JavaStreamingContext call() throws Exception {

                //获取配置中的topic
                String topicStr = serverProps.getProperty("kafka.topic");
                Set<String> topicSet = new HashSet(Arrays.asList(topicStr.split(",")));

                //获取批次的时间间隔，比如5s
                final Long streamingInterval = Long.parseLong(serverProps.getProperty("streaming.interval"));

                //获取checkpoint的hdfs路径
                final String checkpointPath = serverProps.getProperty("streaming.checkpoint.path");

                //获取kafka broker列表
                final String kafkaBrokerList = serverProps.getProperty("kafka.broker.list");

                //获取配置中的groupId
                final String groupId = serverProps.getProperty("kafka.groupId");

                //组合kafka参数
                final Map<String, String> kafkaParams = new HashMap();
                kafkaParams.put("metadata.broker.list", kafkaBrokerList);
                kafkaParams.put("group.id", groupId);

                //从zookeeper中获取每个分区的消费到的offset位置
                final KafkaCluster kafkaCluster = getKafkaCluster(kafkaParams);
                Map<TopicAndPartition, Long> consumerOffsetsLong = getConsumerOffsets(kafkaCluster, groupId, topicSet);
                printZkOffset(consumerOffsetsLong);

                // 创建SparkConf对象
                SparkConf sparkConf = new SparkConf().setMaster("local[1]").setAppName("direct");

                // 优雅停止Spark
                // 暴力停掉sparkstreaming是有可能出现问题的，比如你的数据源是kafka，
                // 已经加载了一批数据到sparkstreaming中正在处理，如果中途停掉，
                // 这个批次的数据很有可能没有处理完，就被强制stop了，
                // 下次启动时候会重复消费或者部分数据丢失。
                sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true");

                // 在Spark的架构中，在网络中传递的或者缓存在内存、硬盘中的对象需要进行序列化操作，序列化的作用主要是利用时间换空间
                sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");

                //增加MyRegistrator类，注册需要用Kryo序列化的类
                sparkConf.set("spark.kryo.registrator", "com.victor.registrator.MyKryoRegistrator");

                // 每秒钟对于每个partition读取多少条数据
                // 如果不进行设置，Spark Streaming会一开始就读取partition中的所有数据到内存，给内存造成巨大压力
                // 设置此参数后可以很好地控制Spark Streaming读取的数据量，也可以说控制了读取的进度
                sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "100");

                // 创建javaStreamingContext，设置5s执行一次
                JavaStreamingContext javaStreamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(streamingInterval));
                javaStreamingContext.checkpoint(checkpointPath);

                //创建kafka DStream
                JavaInputDStream<String> kafkaMessage = KafkaUtils.createDirectStream(
                        javaStreamingContext,
                        String.class,
                        String.class,
                        StringDecoder.class,
                        StringDecoder.class,
                        String.class,
                        kafkaParams,
                        consumerOffsetsLong,
                        new Function<MessageAndMetadata<String, String>, String>() {
                            @Override
                            public String call(MessageAndMetadata<String, String> v1) throws Exception {
                                return v1.message();
                            }
                        }
                );

                //需要把每个批次的offset保存到此变量
                final AtomicReference<OffsetRange[]> offsetRanges = new AtomicReference();

                JavaDStream<String> kafkaMessageDStreamTransform = kafkaMessage.transform(
                        new Function<JavaRDD<String>, JavaRDD<String>>() {
                            @Override
                            public JavaRDD<String> call(JavaRDD<String> rddt) throws Exception {
                                // 表示具有[[OffsetRange]]集合的任何对象，这可以用来访问
                                // 由直Direct Kafka DStream生成的RDD中的偏移量范围
                                OffsetRange[] offsets = ((HasOffsetRanges) rddt.rdd()).offsetRanges();

                                offsetRanges.set(offsets);

                                for (OffsetRange o : offsetRanges.get()) {
                                    log.info("rddoffsetRange:========================================================================");
                                    log.info("rddoffsetRange:topic=" + o.topic()
                                            + ",partition=" + o.partition()
                                            + ",fromOffset=" + o.fromOffset()
                                            + ",untilOffset=" + o.untilOffset()
                                            + ",rddpartitions=" + rddt.getNumPartitions()
                                            + ",isempty=" + rddt.isEmpty()
                                            + ",id=" + rddt.id());
                                }
                                return rddt;
                            }
                        });

                //将kafka中的消息转换成对象并过滤不合法的消息
                JavaDStream<String> kafkaMessageFilter = kafkaMessageDStreamTransform.filter(new Function<String, Boolean>() {
                    @Override
                    public Boolean call(String message) throws Exception {
                        try {
                            UserBehavorRequestModel requestModel = JSONUtil.json2Object(message, UserBehavorRequestModel.class);

                            if (requestModel == null ||
                                    requestModel.getUserId() == 0 ||
                                    requestModel.getSingleUserBehaviorRequestModelList() == null ||
                                    requestModel.getSingleUserBehaviorRequestModelList().size() == 0) {
                                return false;
                            }

                            return true;
                        } catch (Exception e) {
                            log.error("json to UserBehavorRequestModel error", e);
                            return false;
                        }
                    }
                });

                //将每条用户行为转换成键值对，建是我们自定义的key,值是使用应用的时长，并统计时长
                JavaPairDStream<UserHourPackageKey, Long> kafkaStatic = kafkaMessageFilter.flatMapToPair(new PairFlatMapFunction<String, UserHourPackageKey, Long>() {
                    @Override
                    public Iterator<Tuple2<UserHourPackageKey, Long>> call(String s) throws Exception {

                        List<Tuple2<UserHourPackageKey, Long>> list = new ArrayList();
                        UserBehavorRequestModel requestModel = null;

                        try {
                            //将JSON字符串数据转换为UserBehavorRequestModel.class对象
                            requestModel = JSONUtil.json2Object(s, UserBehavorRequestModel.class);
                            //System.out.println(requestModel.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //提取出requestModel中的data数据
                        List<SingleUserBehaviorRequestModel> dataList = requestModel.getSingleUserBehaviorRequestModelList();

                        //对data中的每一项进行处理
                        for (SingleUserBehaviorRequestModel sigleModel : dataList) {
                            //以userId、Hour(yyyyMMddHH)、PackageName组合为key
                            UserHourPackageKey key = new UserHourPackageKey();
                            key.setUserId(requestModel.getUserId());
                            key.setHour(DateUtils.getDateStringByMillisecond(DateUtils.HOUR_FORMAT, requestModel.getBeginTime()));
                            key.setPackageName(sigleModel.getPackageName());
                            //System.out.println(key.toString());

                            //以Package活跃时间为value
                            Tuple2<UserHourPackageKey, Long> t = new Tuple2<UserHourPackageKey, Long>(key, sigleModel.getActiveTime() / 1000);
                            list.add(t);
                        }

                        return list.iterator();
                    }
                    //将一个批次中的同一Package的activeTime聚合到一起
                }).reduceByKey(new Function2<Long, Long, Long>() {
                    @Override
                    public Long call(Long v1, Long v2) throws Exception {
                        return v1 + v2;
                    }
                });

                kafkaStatic.print();

                //将每个用户的统计时长写入hbase
                kafkaStatic.foreachRDD(new VoidFunction<JavaPairRDD<UserHourPackageKey, Long>>() {

                    @Override
                    public void call(JavaPairRDD<UserHourPackageKey, Long> rdd) throws Exception {
                        rdd.foreachPartition(new VoidFunction<Iterator<Tuple2<UserHourPackageKey, Long>>>() {

                            @Override
                            public void call(Iterator<Tuple2<UserHourPackageKey, Long>> it) throws Exception {

                                BehaviorStatService service = BehaviorStatService.getInstance(serverProps);

                                while (it.hasNext()) {
                                    Tuple2<UserHourPackageKey, Long> t = it.next();
                                    UserHourPackageKey key = t._1();

                                    //创建用户行为统计模型
                                    UserBehaviorStatModel model = new UserBehaviorStatModel();
                                    //根据key中的数据填充统计模型
                                    model.setUserId(MyStringUtil.getFixedLengthStr(key.getUserId() + "", 10));
                                    model.setHour(key.getHour());
                                    model.setPackageName(key.getPackageName());
                                    model.setTimeLen(t._2());

                                    //根据统计模型中的数据，对HBase表中的数据进行更新
                                    service.addTimeLen(model);
                                }
                            }
                        });

                        //kafka offset写入zk
                        offsetToZk(kafkaCluster, offsetRanges, groupId);
                    }
                });
                return javaStreamingContext;
            }
        };
        return createContextFunc;
    }

    /*
    * 将offset写入zk
    *
    */
    public static void offsetToZk(final KafkaCluster kafkaCluster,
                                  final AtomicReference<OffsetRange[]> offsetRanges,
                                  final String groupId) {
        for (OffsetRange o : offsetRanges.get()) {

            // 封装topic.partition 与 offset对应关系 java Map
            TopicAndPartition topicAndPartition = new TopicAndPartition(o.topic(), o.partition());
            Map<TopicAndPartition, Object> topicAndPartitionObjectMap = new HashMap();
            topicAndPartitionObjectMap.put(topicAndPartition, o.untilOffset());

            // 转换java map to scala immutable.map
            scala.collection.mutable.Map<TopicAndPartition, Object> testMap = JavaConversions.mapAsScalaMap(topicAndPartitionObjectMap);

            scala.collection.immutable.Map<TopicAndPartition, Object> scalatopicAndPartitionObjectMap =
                    testMap.toMap(new Predef.$less$colon$less<Tuple2<TopicAndPartition, Object>, Tuple2<TopicAndPartition, Object>>() {
                        @Override
                        public Tuple2<TopicAndPartition, Object> apply(Tuple2<TopicAndPartition, Object> v1) {
                            return v1;
                        }
                    });

            // 更新offset到kafkaCluster
            kafkaCluster.setConsumerOffsets(groupId, scalatopicAndPartitionObjectMap);
        }
    }

    /*
    * 获取kafka每个分区消费到的offset,以便继续消费
    * */
    public static Map<TopicAndPartition, Long> getConsumerOffsets(KafkaCluster kafkaCluster, String groupId, Set<String> topicSet) {

        scala.collection.mutable.Set<String> mutableTopics = JavaConversions.asScalaSet(topicSet);
        scala.collection.immutable.Set<String> immutableTopics = mutableTopics.toSet();
        scala.collection.immutable.Set<TopicAndPartition> topicAndPartitionSet2 = (scala.collection.immutable.Set<TopicAndPartition>) kafkaCluster.getPartitions(immutableTopics).right().get();

        // kafka direct stream 初始化时使用的offset数据
        Map<TopicAndPartition, Long> consumerOffsetsLong = new HashMap();

        // 没有保存offset时（该group首次消费时）, 各个partition offset 默认为0
        if (kafkaCluster.getConsumerOffsets(groupId, topicAndPartitionSet2).isLeft()) {

            Set<TopicAndPartition> topicAndPartitionSet1 = JavaConversions.setAsJavaSet(topicAndPartitionSet2);

            for (TopicAndPartition topicAndPartition : topicAndPartitionSet1) {
                consumerOffsetsLong.put(topicAndPartition, 0L);
            }
        } else { // offset已存在, 使用保存的offset
            scala.collection.immutable.Map<TopicAndPartition, Object> consumerOffsetsTemp =
                    (scala.collection.immutable.Map<TopicAndPartition, Object>) kafkaCluster.getConsumerOffsets(groupId, topicAndPartitionSet2).right().get();

            Map<TopicAndPartition, Object> consumerOffsets = JavaConversions.mapAsJavaMap(consumerOffsetsTemp);

            Set<TopicAndPartition> topicAndPartitionSet1 = JavaConversions.setAsJavaSet(topicAndPartitionSet2);

            for (TopicAndPartition topicAndPartition : topicAndPartitionSet1) {
                Long offset = (Long) consumerOffsets.get(topicAndPartition);
                consumerOffsetsLong.put(topicAndPartition, offset);
            }
        }

        return consumerOffsetsLong;
    }

    //获取KafkaCluster
    public static KafkaCluster getKafkaCluster(Map<String, String> kafkaParams) {
        scala.collection.mutable.Map<String, String> testMap = JavaConversions.mapAsScalaMap(kafkaParams);
        scala.collection.immutable.Map<String, String> scalaKafkaParam =
                testMap.toMap(new Predef.$less$colon$less<Tuple2<String, String>, Tuple2<String, String>>() {
                    @Override
                    public Tuple2<String, String> apply(Tuple2<String, String> v1) {
                        return v1;
                    }
                });

        return new KafkaCluster(scalaKafkaParam);
    }

    /**
     * 打印配置文件
     *
     * @param serverProps
     */
    public static void printConfig(Properties serverProps) {
        Iterator<Map.Entry<Object, Object>> it1 = serverProps.entrySet().iterator();
        while (it1.hasNext()) {
            Map.Entry<Object, Object> entry = it1.next();
            log.info(entry.getKey().toString() + "=" + entry.getValue().toString());
        }
    }

    /**
     * 打印从zookeeper中获取的每个分区消费到的位置
     *
     * @param consumerOffsetsLong
     */
    public static void printZkOffset(Map<TopicAndPartition, Long> consumerOffsetsLong) {
        Iterator<Map.Entry<TopicAndPartition, Long>> it1 = consumerOffsetsLong.entrySet().iterator();
        while (it1.hasNext()) {
            Map.Entry<TopicAndPartition, Long> entry = it1.next();
            TopicAndPartition key = entry.getKey();
            log.info("zookeeper offset:partition=" + key.partition() + ",beginOffset=" + entry.getValue());
        }
    }
}
