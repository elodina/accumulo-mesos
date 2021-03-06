#!/bin/bash

export LOG=/tmp/accumulo-framework.log

export ACCUMULO_HOME=/vagrant/dev/dist/accumulo-1.7.0
export ACCUMULO_CLIENT_CONF_PATH=$ACCUMULO_HOME/conf
export HADOOP_PREFIX=/usr/local/hadoop
export HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
export ZOOKEEPER_HOME=/etc/zookeeper

java -jar /vagrant/dev/dist/accumulo-mesos-dist-0.2.0-SNAPSHOT/accumulo-mesos-framework-0.2.0-SNAPSHOT-jar-with-dependencies.jar \
     -master 172.16.0.100:5050 \
     -zookeepers 172.16.0.100:2181 \
     -name accumulo-mesos-test-1 \
    | tee $LOG


#    "bindAddress": "172.16.0.100",
#    "httpPort": "8192",
#    "mesosMaster": "172.16.0.100:5050",
#    "name":"accumulo-mesos-test",
#    "id": "",
#    "tarballUri": "hdfs://172.16.0.100:9000/dist/accumulo-mesos-dist-0.2.0-SNAPSHOT.tar.gz",
#    "zkServers": "172.16.0.100:2181"
