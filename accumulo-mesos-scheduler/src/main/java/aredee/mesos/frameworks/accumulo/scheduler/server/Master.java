package aredee.mesos.frameworks.accumulo.scheduler.server;

import aredee.mesos.frameworks.accumulo.configuration.ServerType;

public class Master extends BaseServer {

    public Master(String taskId, String slaveId) {
        super(taskId, slaveId);
    }

    public Master(String taskId){
        super(taskId);
    }

    @Override
    public ServerType getType(){ return ServerType.MASTER; }
}
