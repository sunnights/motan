package com.weibo.api.motan.transport.netty4.reactor;

import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;

/**
 * @author sunnights
 */
public class MotanOperations<INBOUND extends NettyInbound, OUTBOUND extends NettyOutbound>
        extends ChannelOperations<INBOUND, OUTBOUND> implements MotanInfos {
    protected MotanOperations(ChannelOperations<INBOUND, OUTBOUND> replaced) {
        super(replaced);
    }

    public MotanOperations(Connection connection, ConnectionObserver listener) {
        super(connection, listener);
        //reset channel to manual read if re-used
        connection.channel()
                .config()
                .setAutoRead(false);
    }
}
