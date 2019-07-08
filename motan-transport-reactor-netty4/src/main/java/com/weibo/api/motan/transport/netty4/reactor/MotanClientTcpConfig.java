package com.weibo.api.motan.transport.netty4.reactor;

import reactor.netty.tcp.TcpClient;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author sunnights
 */
public class MotanClientTcpConfig extends MotanClientOperator {
    final Function<? super TcpClient, ? extends TcpClient> tcpClientMapper;

    public MotanClientTcpConfig(MotanClient client, Function<? super TcpClient, ? extends TcpClient> tcpClientMapper) {
        super(client);
        this.tcpClientMapper = Objects.requireNonNull(tcpClientMapper, " tcpClientMapper");
    }

    @Override
    protected TcpClient tcpConfiguration() {
        return Objects.requireNonNull(tcpClientMapper.apply(source.tcpConfiguration()), "tcpClientMapper");
    }
}
