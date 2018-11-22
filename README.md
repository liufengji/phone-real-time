
#conf 
##配置文件

#phone项目code
## taildirsource flume1.6 源码修改移植新功能
## log-collector-web web项目，产生用户行为日志 war包不能直接依赖code,需要maven install
## data_producer 模仿用户行为，访问web项目
## common 工具包
## spark-streaming java code
   #### 读取kafka 读取zookeeper -> ETL处理数据 -> 存入hbase -> offset 存入zookeeper

# nginx
## mginx停止： nginx -s stop
## nginx启动： sudo /usr/local/nginx/sbin/nginx -c conf/nginx.conf

# tomcat
## tomcat启动 bin/start.sh
## tomcat关闭 bin/shutdown.sh

# flume
## bin/flume-ng agent --classpath lib/flume-taildirsource.jar --conf conf/ -f job/flume1a.conf -n a1
## bin/flume-ng agent --classpath lib/flume-taildirsource.jar --conf conf/ -f job/flume1b.conf -n a1
## bin/flume-ng agent --conf conf/ -f job/flume2a.conf -n a1
## bin/flume-ng agent --conf conf/ -f job/flume2b.conf -n a1

#zookeeper
## sh /opt/module/zookeeper-3.4.10/bin/zkServer.sh start
## sh /opt/module/zookeeper-3.4.10/bin/zkServer.sh status
## sh /opt/module/zookeeper-3.4.10/bin/zkServer.sh stop

# kafka
## kafka 启动 nohup bin/kafka-server-start.sh -daemon /opt/module/kafka/config/server.properties &
## kafka 关闭 bin/kafka-server-stop.sh config/server.properties
## kafka 关闭 kill -9 8953
## kafka 创建topic bin/kafka-topics.sh --create --zookeeper hadoop102:2181,hadoop103:2181,hadoop104:2181 --replication-factor 3 --partitions 3 --topic t-behavior
## kafka 查看topic bin/kafka-topics.sh --zookeeper hadoop102:2181,hadoop103:2181,hadoop103:2181 --list
## kafka 控制台消费topic bin/kafka-console-consumer.sh --zookeeper hadoop102:2181,hadoop103:2181,hadoop104:2181 --from-beginning --topic t-behavior


#hbase
##bin/hbase shell
##create 'behavior_user_app_201712','timeLen'
##create 'behavior_user_day_app_time_201712','timeLen'
##create 'behavior_user_day_time_201712','timeLen'
##create 'behavior_user_hour_app_time_201712','timeLen'
##create 'behavior_user_hour_time_201712','timeLen'
##alter 'behavior_user_app_201712', NAME => 'info', VERSIONS => 2