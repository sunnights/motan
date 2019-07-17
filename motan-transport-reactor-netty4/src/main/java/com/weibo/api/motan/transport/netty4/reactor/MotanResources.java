package com.weibo.api.motan.transport.netty4.reactor;

import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * @author sunnights
 */
public class MotanResources extends TcpResources {
    private static final AtomicReference<MotanResources> MOTAN_RESOURCES;
    private static final BiFunction<LoopResources, ConnectionProvider, MotanResources> ON_MOTAN_NEW;

    static {
        MOTAN_RESOURCES = new AtomicReference<>();
        ON_MOTAN_NEW = MotanResources::new;
    }

    private MotanResources(LoopResources defaultLoops, ConnectionProvider defaultProvider) {
        super(defaultLoops, defaultProvider);
    }

    public static MotanResources get() {
        ConnectionProvider provider = ConnectionProvider.fixed("motan-conn", DEFAULT_POOL_MAX_CONNECTIONS, 1000);
        return getOrCreate(MOTAN_RESOURCES, null, provider, ON_MOTAN_NEW, "motan-tcp");
    }
}
