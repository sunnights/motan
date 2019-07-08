package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.NettyMessage;
import com.weibo.api.motan.util.LoggerUtil;
import io.netty.channel.ChannelHandlerContext;
import reactor.core.publisher.Mono;
import reactor.netty.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author sunnights
 */
public class MotanClientOperations extends MotanOperations<NettyInbound, NettyOutbound> {
    Request request;
    Response response;
    Codec codec;
    URL url;

    public MotanClientOperations(Connection ch, ConnectionObserver c, Codec codec, URL url) {
        super(ch, c);
        request = new DefaultRequest();
        this.codec = codec;
        this.url = url;
    }

    final Mono<Void> send() {
        return FutureMono.deferFuture(() -> channel().writeAndFlush(request));
    }

    @Override
    protected void onInboundNext(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof NettyMessage) {
            processMessage(ctx, (NettyMessage) msg);
            return;
        }
        super.onInboundNext(ctx, msg);
    }

    private void processMessage(ChannelHandlerContext ctx, NettyMessage msg) {
        String remoteIp = getRemoteIp(ctx);
        Object result;

        try {
            result = codec.decode(new SimpleChannel(url), remoteIp, msg.getData());
        } catch (Exception e) {
            LoggerUtil.error("NettyDecoder decode fail! requestid" + msg.getRequestId() + ", size:" + msg.getData().length + ", ip:" + remoteIp + ", e:" + e.getMessage());
            return;
        }
        if (result instanceof Response) {
            setResponse((Response) result);
            listener().onStateChange(this, MotanClientState.RESPONSE_RECEIVED);
        }
    }

    @Override
    protected void onOutboundComplete() {
        if (isInboundCancelled()) {
            return;
        }
        listener().onStateChange(this, MotanClientState.REQUEST_SENT);
        channel().read();
    }

    private String getRemoteIp(ChannelHandlerContext ctx) {
        String ip = "";
        SocketAddress remote = ctx.channel().remoteAddress();
        if (remote != null) {
            try {
                ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            } catch (Exception e) {
                LoggerUtil.warn("get remoteIp error! default will use. msg:{}, remote:{}", e.getMessage(), remote.toString());
            }
        }
        return ip;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
