package aredee.mesos.frameworks.accumulo.initialize;

import aredee.mesos.frameworks.accumulo.configuration.Environment;
import aredee.mesos.frameworks.accumulo.model.Accumulo;
import aredee.mesos.frameworks.accumulo.process.AccumuloProcessFactory;
import org.apache.accumulo.server.init.Initialize;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;

public class AccumuloInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccumuloInitializer.class);

    private Accumulo config;
    private String accumuloHome;
    
    public AccumuloInitializer(Accumulo config) throws Exception {
        this.config = config;
        this.accumuloHome = Environment.get(Environment.ACCUMULO_HOME);
    }
    
    /**
     * Write the accumulo site file and initialize accumulo.
     * 
     * The system property Environment.ACCUMULO_HOME and HADOOP_CONF_DIR must be set and config must have
     * the accumulo instance name, root password, along with the accumulo directories set.
     *
     * @return accumulo instance name
     */
    public void initialize() throws Exception{
        
        // run accumulo init procedure
        LOGGER.info("Writing accumulo-site.xml");

        AccumuloSiteXml siteXml = new AccumuloSiteXml(this.config);
        siteXml.initializeFromScheduler(AccumuloSiteXml.getEmptySiteXml());

        writeAccumuloSiteFile(accumuloHome, siteXml);
        config.setSiteXml(siteXml.toString());

        LinkedList<String> initArgs  = new LinkedList<>();
        initArgs.add("--instance-name");
        initArgs.add(config.getInstance());
        initArgs.add("--password");
        initArgs.add(config.getRootPassword());
        initArgs.add("--user");
        initArgs.add(config.getRootUser());

        // This clears the instance name out of zookeeper, this may need revisited, but was
        // needed during testing.
        initArgs.add("--clear-instance-name");

        AccumuloProcessFactory processFactory = new AccumuloProcessFactory("256");
       
        Process initProcess = null;
        try {
            initProcess = processFactory.exec(Initialize.class, null, initArgs.toArray(new String[initArgs.size()]));
            LOGGER.info("Initializing Accumulo");
            initProcess.waitFor();
            LOGGER.info("New Accumulo instance initialized: {}", config.getInstance() );
        } catch (Exception ioe) {
            LOGGER.error("IOException while trying to initialize Accumulo", ioe);
            System.exit(-1);
        }  
        return;
    }

/*
    public static AccumuloSiteXml createAccumuloSiteXml(String xml) throws Exception {
        return new AccumuloSiteXml(new ByteArrayInputStream(xml.getBytes()));
    }
*/

    public static void writeAccumuloSiteFile(String accumuloHomeDir, AccumuloSiteXml siteXml) {
        LOGGER.info("ACCUMULO HOME? " + accumuloHomeDir);
        try {
  
            File accumuloSiteFile = new File(accumuloHomeDir + File.separator +
                    "conf" + File.separator + "accumulo-site.xml");

            LOGGER.info("Writing accumulo-site.xml to {}", accumuloSiteFile.getAbsolutePath());

            OutputStream siteFile = new FileOutputStream(accumuloSiteFile);
            IOUtils.write(siteXml.toXml(), siteFile);
            IOUtils.closeQuietly(siteFile);

        } catch (Exception e) {
            logErrorAndDie("Error Creating accumulo-site.xml\n",e);
        }
    }

    private static void logErrorAndDie(String message, Exception e){
        LOGGER.error(message, e);
        throw new RuntimeException(message, e);
    }


}
