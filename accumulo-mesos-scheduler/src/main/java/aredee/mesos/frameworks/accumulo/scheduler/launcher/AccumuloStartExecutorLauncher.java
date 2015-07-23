package aredee.mesos.frameworks.accumulo.scheduler.launcher;

import aredee.mesos.frameworks.accumulo.configuration.cluster.ClusterConfiguration;
import aredee.mesos.frameworks.accumulo.configuration.Environment;
import aredee.mesos.frameworks.accumulo.configuration.ServiceProcessConfiguration;
import aredee.mesos.frameworks.accumulo.scheduler.matcher.Match;
import aredee.mesos.frameworks.accumulo.scheduler.server.AccumuloServer;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Environment.Variable.Builder;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Value.Type;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches an Executor process that starts Accumulo Servers using the accumulo-start jar.
 */
public class AccumuloStartExecutorLauncher implements Launcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AccumuloStartExecutorLauncher.class);
    
    private ClusterConfiguration config;
    private ServiceProcessConfiguration serviceConfig;
    
    public AccumuloStartExecutorLauncher(ServiceProcessConfiguration serviceConfig, ClusterConfiguration config){
        this.config = config;
        this.serviceConfig = serviceConfig;
    }

    /**
     * Interface used to launch Accumulo Server tasks.
     *
     * @param driver Mesos interface to use to launch a server
     * @param match AccumuloServer and Offer to launch
     */
    public Protos.TaskInfo launch(SchedulerDriver driver, Match match){
        AccumuloServer server = match.getServer();
        Protos.Offer offer = match.getOffer();

        String args[] = new String[1];
        args[0] = server.getType().getName();

        List<Protos.CommandInfo.URI> uris = new ArrayList<>();
        Protos.CommandInfo.URI tarballUri = Protos.CommandInfo.URI.newBuilder()
                .setValue(this.config.getAccumuloTarballUri())
                .setExtract(true)
                .setExecutable(false)
                .build();

        Protos.CommandInfo.URI executorJarUri = Protos.CommandInfo.URI.newBuilder()
                .setValue(this.config.getExecutorJarUri())
                .setExtract(true)
                .setExecutable(false)
                .build();

        uris.add(tarballUri);
        uris.add(executorJarUri);

        LOGGER.info("Executor jar location " + executorJarUri);
   
        // TODO get java -XX stuff from config
        // TODO get executor jar name from URI
        // The "m" is hard coded here to get the cluster up...should be handled in another manner, maybe another method that
        // leveraged off the *ExecutorMemory, or if this is not being used to check against offers then just make it the
        // correct string....
        
        // Since JAVA_HOME is usually installed here...hard code it for now. Should we pass it in or instead
        // of launching it directly use a script that checks the local server(environment) for JAVA_HOME...and
        // the rest of the environment var?
        StringBuilder sb = new StringBuilder("/usr/bin/java")
                .append(" -Dserver=").append(server.getType().getName())
                .append(" -Xmx").append(((int)this.config.getMaxExecutorMemory())+"m")
                .append(" -Xms").append(((int)this.config.getMinExecutorMemory())+"m")
                .append(" -jar ").append(getExecutorJarFromURI(this.config.getExecutorJarUri()));

        Builder varBuilder = Protos.Environment.Variable.newBuilder();
        Protos.Environment env = Protos.Environment.newBuilder()
                .addVariables(varBuilder
                        .setName(Environment.HADOOP_PREFIX)
                        .setValue(serviceConfig.getHadoopHomeDir().getAbsolutePath()))
                .addVariables(varBuilder
                        .setName(Environment.HADOOP_CONF_DIR)
                        .setValue(serviceConfig.getAccumuloConfDir().getAbsolutePath()))
                 .addVariables(varBuilder
                        .setName(Environment.ZOOKEEPER_HOME)
                        .setValue(serviceConfig.getZooKeeperDir().getAbsolutePath()))
                .build();
                
        Protos.CommandInfo commandInfo = Protos.CommandInfo.newBuilder()
                .setValue(sb.toString())
                .setEnvironment(env)
                .addAllUris(uris)
                .build();
        
        // configure the server
        aredee.mesos.frameworks.accumulo.Protos.ServerProcessConfiguration serverConfig
                = aredee.mesos.frameworks.accumulo.Protos.ServerProcessConfiguration.newBuilder()
                .setServerType(server.getType().getName())
                .setMaxMemory(server.getMaxMemorySize())
                .setMinMemory(server.getMinMemorySize())
                .build();      
        
        Scalar executorMem = Value.Scalar.newBuilder().setValue(config.getMaxExecutorMemory()).build();

        // TODO only get desired resources of offer
        Protos.ExecutorInfo executorInfo = Protos.ExecutorInfo.newBuilder()
                .setExecutorId(ExecutorID.newBuilder().setValue(match.getServer().getSlaveId()))
                .setCommand(commandInfo)
                .setData(serverConfig.toByteString())  // serialize model here.
                         .addResources(Resource.newBuilder()
                              .setName("cpus")
                              .setType(Type.SCALAR)
                              .setScalar(Scalar.newBuilder().setValue(1))
                              .setRole("*"))
                          .addResources(Resource.newBuilder()
                              .setName("mem")
                              .setType(Type.SCALAR)
                              .setScalar(executorMem)
                              .setRole("*"))
                .setName("accumuloExecutor-1")
                .build();

        Protos.TaskInfo taskInfo = Protos.TaskInfo.newBuilder()
                .setName(server.getType().getName())
                .setTaskId(Protos.TaskID.newBuilder().setValue(server.getTaskId()))
                .setSlaveId(Protos.SlaveID.newBuilder().setValue(server.getSlaveId()))
                .setData(serverConfig.toByteString())  // serialize model here.
                         .addResources(Resource.newBuilder()
                              .setName("cpus")
                              .setType(Type.SCALAR)
                              .setScalar(Scalar.newBuilder().setValue(1))
                              .setRole("*"))
                          .addResources(Resource.newBuilder()
                              .setName("mem")
                              .setType(Type.SCALAR)
                              .setScalar(executorMem)
                              .setRole("*"))                
                              .setExecutor(executorInfo)               
                .build();
                   
        // TODO handle driver Status
        Protos.Status status = driver.launchTasks(Arrays.asList(new Protos.OfferID[]{offer.getId()}),
                Arrays.asList(new Protos.TaskInfo[]{taskInfo}));

        return taskInfo;
    }

    public String getExecutorJarFromURI(String uriString){
        String[] parts = uriString.split(File.pathSeparator);
        return parts[parts.length-1];
    }
}
