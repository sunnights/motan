package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
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

    public final MotanClient port(int port) {
        return tcpConfiguration(tcpClient -> tcpClient.port(port));
    }

    public final MotanClient codec(String codec) {
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.codec(bootstrap, codec)));
    }

    public final MotanClient minClientConnection(Integer minClientConnection) {
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.minClientConnection(bootstrap, minClientConnection)));
    }

    public final MotanClient maxClientConnection(Integer maxClientConnection) {
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.maxClientConnection(bootstrap, maxClientConnection)));
    }

    public final MotanClient maxContentLength(Integer maxContentLength) {
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.maxContentLength(bootstrap, maxContentLength)));
    }

    public final MotanClient url(URL url) {
        return tcpConfiguration(tcpClient -> tcpClient.bootstrap(bootstrap -> MotanClientConfiguration.url(bootstrap, url)));
    }

    public RequestSender build() {
        return new MotanClientFinalizer(tcpConfiguration());
    }

    public interface RequestSender extends ResponseReceiver<RequestSender> {
        ResponseReceiver<?> send(Mono<? extends Request> request);
    }

    public interface ResponseReceiver<S extends ResponseReceiver<?>> {
        Mono<Response> response();
    }

}
