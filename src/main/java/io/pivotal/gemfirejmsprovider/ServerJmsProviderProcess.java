package io.pivotal.gemfirejmsprovider;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.timewalker.ffmq4.FFMQServer;
import net.timewalker.ffmq4.utils.Settings;

public class ServerJmsProviderProcess implements Function, Declarable {

    private static final long serialVersionUID = 1L;

    private static final int SHUTDOWN_TIMEOUT = 60; // seconds
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerJmsProviderProcess.class);

    @Override
    public void init(Properties arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void execute(FunctionContext context) {
        if (!(context instanceof RegionFunctionContext)) {
            throw new FunctionException(
                    "This is a data aware function, and has to be called using FunctionService.onRegion.");
        }
        RegionFunctionContext regionFunctionContext = (RegionFunctionContext) context;

        Map<String, Object> arguments = (Map<String, Object>) regionFunctionContext.getArguments();

        LOG.info("Executing server-side JMS provider");
        
        // Get a handle on the server cache object
        Cache cache = CacheFactory.getAnyInstance();

        // Get handles on the live and export region objects
        Region<String, String> jmsRegion = cache.getRegion("jms");
        
        LOG.debug(arguments.values().toString());

        // TODO
        setupFfmqSystemProperties();
        Settings settings = new Settings(new ConfigProperties().get());

        try {
            // Create a server instance
            FFMQServer server = new FFMQServer("engine1", settings);
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(server));

            // Start server
            server.run();
        } catch (javax.jms.JMSException e) {
            LOG.info("Exception executing server-side JMS provided: " + e.toString());
            return;
        }
    }

    private static void setupFfmqSystemProperties()
    {
        // Application home
        String ffmqHome = System.getProperty("FFMQ_HOME");
        if (ffmqHome == null)
        {
            ffmqHome = "..";
            System.setProperty("FFMQ_HOME", ffmqHome);
        }

        // Application base
        String ffmqBase = System.getProperty("FFMQ_BASE");
        if (ffmqBase == null)
            System.setProperty("FFMQ_BASE", ffmqHome);
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public boolean isHA() {
        return false;
    }

    @Override
    public boolean optimizeForWrite() {
        return true;
    }

    private static class ShutdownHook extends Thread
    {
    	private FFMQServer server;
    	
    	/**
		 * Constructor
		 */
		public ShutdownHook( FFMQServer server )
		{
			super();
			this.server = server;
		}
    	
    	/* (non-Javadoc)
    	 * @see java.lang.Thread#run()
    	 */
    	@Override
		public void run()
    	{
    		try
    		{
	    		if (server.isStarted())
	    		{
	    			LOG.info("Caught signal, asking the JMS provider to shutdown");
	    			server.pleaseStop();
	    			
	    			// Wait for the server to stop
	    			long startTime = System.currentTimeMillis();
	    			while (server.isStarted())
	    			{
	    				Thread.sleep(100);
	    				
	    				long now = System.currentTimeMillis();
	    				if (now-startTime > SHUTDOWN_TIMEOUT*1000)
	    				{
	    					LOG.error("Timeout waiting for JMS provider shutdown ("+SHUTDOWN_TIMEOUT+"s)");
	    					return;
	    				}
	    			}
	    		}
    		}
    		catch (Throwable e)
    		{
    			LOG.error("Cannot shutdown JMS provider",e);
    		}
    	}
    }  
}
