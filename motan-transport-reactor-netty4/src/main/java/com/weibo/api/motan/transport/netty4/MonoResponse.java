package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.rpc.AbstractMonoResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import io.netty.channel.ChannelFuture;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

/**
 * @author sunnights
 */
public class MonoResponse extends AbstractMonoResponse {
    NettyChannel nettyChannel;
    Request request;

    public MonoResponse(NettyChannel nettyChannel, Request request) {
        this.nettyChannel = nettyChannel;
        this.request = request;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Response> actual) {
        Mono.<Response>create(sink -> {
            // add listener
            nettyChannel.getNettyClient().listenerMap.put(request.getRequestId(), new ChannelListener() {
                @Override
                public void onStateChange(Response response) {
                    sink.success(response);
                }
            });

            byte[] msg = CodecUtil.encodeObjectToBytes(nettyChannel, nettyChannel.getCodec(), request);
            ChannelFuture writeFuture = nettyChannel.getChannel().writeAndFlush(msg);
        }).subscribe(actual);
    }
}
