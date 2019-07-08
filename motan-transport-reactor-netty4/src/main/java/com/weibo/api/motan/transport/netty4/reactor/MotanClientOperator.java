package com.weibo.api.motan.transport.netty4.reactor;

import reactor.netty.tcp.TcpClient;

import java.util.Objects;

/**
 * @author sunnights
 */
public class MotanClientOperator extends MotanClient {
    final MotanClient source;

    public MotanClientOperator(MotanClient source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    protected TcpClient tcpConfiguration() {
        return source.tcpConfiguration();
    }
}
