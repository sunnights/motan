package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.netty4.CodecUtil;
import io.netty.bootstrap.Bootstrap;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.tcp.TcpClient;

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
        Mono<ChannelOperations<?, ?>> connector = (Mono<ChannelOperations<?, ?>>) cachedConfiguration.connect(b);
        return connector.map(ops -> {
//            if (!ops.isInboundDisposed()) {
//                ops.discard();
//            }
            if (ops instanceof MotanClientOperations) {
                return ((MotanClientOperations) ops).getResponse();
            }
            return null;
        });
    }
}
