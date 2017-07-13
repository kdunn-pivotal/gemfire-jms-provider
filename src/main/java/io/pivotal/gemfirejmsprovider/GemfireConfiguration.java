package io.pivotal.gemfirejmsprovider;

import javax.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.pivotal.gemfirejmsprovider.ServerJmsProviderProcess;

@Configuration
public class GemfireConfiguration {
    @Value("${locator-host:localhost}")
    private String locatorHost;

    @Value("${locator-port:20334}")
    private int locatorPort;

    @Value("${cache-server.port:40404}")
    private int port;
    
    @Bean
    public Cache createCache() throws Exception {
        CacheFactory cf = new CacheFactory();
        //cf.set("name", "ServerFunctionTdd");
        cf.set("start-locator", locatorHost + 
            "[" + locatorPort + "]");

        //cf.setPdxSerializer(new ReflectionBasedAutoSerializer("io.pivotal.tutorial.model.*"));

        Cache c = cf.create();
        CacheServer cs = c.addCacheServer();
        cs.setPort(port);
        cs.start();
        
        return c;
    }
    
    @Bean
    public Region<String, String> jmsRegion(Cache cache) {

        RegionFactory<String, String> rf = cache.createRegionFactory(RegionShortcut.PARTITION);
        
        return rf.create("jms");
    }

    @Bean
    public String isRunning(Region<String, String> jmsRegion) {
        executeFunction(jmsRegion);

        return new String("Nothing");
    }

    //@PostConstruct
    public void executeFunction(Region<String, String> jmsRegion) {

        FunctionService.registerFunction(new ServerJmsProviderProcess());
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("keys", "abc");

        // Start the FFMQ4 JMS provider process
        FunctionService
                .onRegion(jmsRegion)
                .withArgs(args)
                .execute("ServerJmsProviderProcess");
    }

}
