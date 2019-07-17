package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;

import java.util.function.Function;

/**
 * @author sunnights
 */
public abstract class MotanClient {
    private static final TcpClient DEFAULT_TCP_CLIENT = TcpClient.newConnection();

    public static MotanClient create() {
        return create(MotanResources.get());
    }

    private static MotanClient create(ConnectionProvider connectionProvider) {
        return new MotanClientConnect(TcpClient.create(connectionProvider));
    }

    protected TcpClient tcpConfiguration() {
        return DEFAULT_TCP_CLIENT;
    }

    public final MotanClient tcpConfiguration(Function<? super TcpClient, ? extends TcpClient> tcpMapper) {
        return new MotanClientTcpConfig(this, tcpMapper);
    }

    public final MotanClient url(URL url) {
        tcpConfiguration(tcpClient -> tcpClient.port(url.getPort()));
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.url(bootstrap, url))
                .port(url.getPort()));
    }

    public RequestSender build() {
        return new MotanClientFinalizer(tcpConfiguration());
    }

    public interface RequestSender extends ResponseReceiver<RequestSender>, Client {
        ResponseReceiver<?> send(Mono<? extends Request> request);
    }

    public interface ResponseReceiver<S extends ResponseReceiver<?>> {
        Mono<Response> response();
    }
}
