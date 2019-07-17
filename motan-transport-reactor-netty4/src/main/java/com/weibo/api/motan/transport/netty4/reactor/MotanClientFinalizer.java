package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.transport.netty4.CodecUtil;
import io.netty.bootstrap.Bootstrap;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpClient;

import java.net.InetSocketAddress;
import java.util.function.BiFunction;

/**
 * @author sunnights
 */
public class MotanClientFinalizer extends MotanClient implements MotanClient.RequestSender {
    private final TcpClient cachedConfiguration;

    public MotanClientFinalizer(TcpClient cachedConfiguration) {
        this.cachedConfiguration = cachedConfiguration;
    }

    @Override
    public MotanClientFinalizer send(Mono<? extends Request> request) {
        return send((handler, outbound) -> {
            Mono<byte[]> msg = request
                    .map(req -> CodecUtil.encodeObjectToBytes(new SimpleChannel(handler.url), handler.codec, req));
            return outbound.sendByteArray(msg);
        });
    }

    public MotanClientFinalizer send(BiFunction<MotanClientConnect.MotanClientHandler, ? super NettyOutbound, ? extends Publisher<Void>> sender) {
        return new MotanClientFinalizer(cachedConfiguration.bootstrap(b -> MotanClientConfiguration.body(b, sender)));
    }

    @Override
    public Mono<Response> response() {
        Bootstrap b = cachedConfiguration.configure();
        MotanClientConnect.MonoMotanConnect connector = (MotanClientConnect.MonoMotanConnect) cachedConfiguration.connect(b);
        return connector.map(ops -> {
            if (ops instanceof MotanClientOperations) {
                MotanClientOperations motanClientOperations = (MotanClientOperations) ops;
                if (!motanClientOperations.isInboundDisposed()) {
                    motanClientOperations.discard();
                }
                return motanClientOperations.getResponse();
            }
            return null;
        });
    }

    @Override
    public void heartbeat(Request request) {
        System.out.println("heartbeat...");
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        System.out.println("getLocalAddress...");
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        System.out.println("getRemoteAddress...");
        return null;
    }

    @Override
    public Response request(Request request) throws TransportException {
        System.out.println("request...");
        return send(Mono.just(request))
                .response().block();
    }

    @Override
    public boolean open() {
        System.out.println("open...");
        return true;
    }

    @Override
    public void close() {
        System.out.println("close...");

    }

    @Override
    public void close(int timeout) {
        System.out.println("close...");

    }

    @Override
    public boolean isClosed() {
        System.out.println("isClosed...");
        return false;
    }

    @Override
    public boolean isAvailable() {
        System.out.println("isAvailable...");
        return true;
    }

    @Override
    public URL getUrl() {
        System.out.println("getUrl...");
        return null;
    }
}
