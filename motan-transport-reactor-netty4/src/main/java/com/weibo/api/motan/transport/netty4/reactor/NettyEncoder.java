package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.protocol.v2motan.MotanV2Codec;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

/**
 * @author sunnights
 */
public class NettyEncoder extends MessageToByteEncoder<Request> {
    private Codec codec;
    private URL url;

    public NettyEncoder(Codec codec, URL url) {
        this.codec = codec;
        this.url = url;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Request msg, ByteBuf out) throws Exception {
        byte[] bytes;
        if (codec instanceof MotanV2Codec) {
            bytes = encodeV2(url, codec, msg);
        } else {
            bytes = encodeV1(url, codec, msg);
        }
        out.writeBytes(bytes);
    }

    private byte[] encodeV2(URL url, Codec codec, Object msg) throws IOException {
        return encodeMessage(url, codec, msg);
    }

    private byte[] encodeV1(URL url, Codec codec, Object msg) throws IOException {
        long requestId = getRequestId(msg);
        byte[] data = encodeMessage(url, codec, msg);
        byte[] result = new byte[MotanConstants.NETTY_HEADER + data.length];
        ByteUtil.short2bytes(MotanConstants.NETTY_MAGIC_TYPE, result, 0);
        result[3] = getType(msg);
        ByteUtil.long2bytes(requestId, result, 4);
        ByteUtil.int2bytes(data.length, result, 12);
        System.arraycopy(data, 0, result, MotanConstants.NETTY_HEADER, data.length);
        return result;
    }

    private byte[] encodeMessage(URL url, Codec codec, Object msg) throws IOException {
        Channel channel = new SimpleChannel(url);
        byte[] data;
        if (msg instanceof Response) {
            try {
                data = codec.encode(channel, msg);
            } catch (Exception e) {
                LoggerUtil.error("NettyEncoder encode error, identity=" + channel.getUrl().getIdentity(), e);
                long requestId = getRequestId(msg);
                Response response = buildExceptionResponse(requestId, e);
                data = codec.encode(channel, response);
            }
        } else {
            data = codec.encode(channel, msg);
        }
        if (msg instanceof Request) {
            MotanFrameworkUtil.logEvent((Request) msg, MotanConstants.TRACE_CENCODE);
        } else if (msg instanceof Response) {
            MotanFrameworkUtil.logEvent((Response) msg, MotanConstants.TRACE_SENCODE);
        }
        return data;
    }

    private long getRequestId(Object message) {
        if (message instanceof Request) {
            return ((Request) message).getRequestId();
        } else if (message instanceof Response) {
            return ((Response) message).getRequestId();
        } else {
            return 0;
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }

    private byte getType(Object message) {
        if (message instanceof Request) {
            return MotanConstants.FLAG_REQUEST;
        } else if (message instanceof Response) {
            return MotanConstants.FLAG_RESPONSE;
        } else {
            return MotanConstants.FLAG_OTHER;
        }
    }
}
