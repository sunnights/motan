import com.sun.btrace.AnyType;
import com.sun.btrace.annotations.*;

import java.util.Map;

import static com.sun.btrace.BTraceUtils.*;

/**
 * trace request for netty3 server
 * Created by zhanglei28 on 2018/12/7.
 */
@BTrace
public class MotanServerTrace {
    // trace configs
    private static String traceIp = "10.73.89.48";
    private static String traceIp2 = "127.0.0.1";
    private static int hour = 0;

    @TLS
    private static boolean traceTag;

    // io read
    @TLS
    private static long readStart;
    @TLS
    private static long readDuration;
    @TLS
    private static long decodeStart;
    @TLS
    private static long decodeDuration;
    @TLS
    private static long rid;
    @TLS
    private static long processEventStart;
    @TLS
    private static long ioReadEnd;


    // biz process
    @TLS
    private static long processReqStart;
    @TLS
    private static long processReqEnd;
    @TLS
    private static long invokeStart;
    @TLS
    private static long invokeDuration;
    @TLS
    private static long encodeStart;
    @TLS
    private static long encodeDuration;


    // io write
    @TLS
    private static long ioWriteStart;
    @TLS
    private static long ioWriteEnd;
    @TLS
    private static long nativeWriteStart;
    @TLS
    private static long nativeWriteDuration;

    private static boolean skipTime() {
        return hour < 22;
    }

    private static boolean skipIp(String ip) {
        return indexOf(ip, traceIp) < 0 && indexOf(ip, traceIp2) < 0;
    }

    //================== io read =====================
    //read
    @OnMethod(clazz = "org.jboss.netty.channel.socket.nio.NioWorker", method = "read")
    public static void readStart(AnyType[] args) {
        traceTag = false;
        hour = Numbers.parseInt(timestamp("HH"));
        if (skipTime()) {
            return;
        }
        if (skipIp(str(get(field(classForName("sun.nio.ch.SocketChannelImpl", contextClassLoader()), "remoteAddress"), get(field(classForName("sun.nio.ch.SelectionKeyImpl", contextClassLoader()), "channel"), args[0]))))) {
            return;
        }
        traceTag = true;
        readStart = timeMillis();
    }


    @OnMethod(clazz = "org.jboss.netty.channel.socket.nio.NioWorker", method = "read", location = @Location(Kind.RETURN))
    public static void readEnd(@Duration long time) {
        if (traceTag) {
            readDuration = time;
        }
    }

    // decode
    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyDecoder",
            method = "decode",
            type = "java.lang.Object(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)")
    public static void decodeStart(AnyType[] args) {
        if (traceTag) {
            decodeStart = timeMillis();
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyDecoder",
            method = "decode",
            type = "java.lang.Object(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)",
            location = @Location(Kind.RETURN))
    public static void decodeEnd(@Return AnyType ret, @Duration long time) {
        if (traceTag && ret != null) {
            decodeDuration = time;
            rid = getLong(field(classForName("com.weibo.api.motan.rpc.DefaultRequest", contextClassLoader()), "requestId"), ret);
        }
    }


    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler", method = "messageReceived")
    public static void processEventStart() {
        if (traceTag) {
            processEventStart = timeMillis();
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler", method = "messageReceived", location = @Location(Kind.RETURN))
    public static void processEventEnd(@Duration long time) {
        if (traceTag) {
            ioReadEnd = timeMillis();
            println("rid:" + rid + "--type:ioread--tid:" + threadId(currentThread()) + "--total:" + (ioReadEnd - readStart) + "--read:" + readDuration / 1e6 + ",decode:" + decodeDuration / 1e6 + ",processEvent:" + time / 1e6 + "--readStart:" + readStart + ",decodeStart:" + decodeStart + ",processEventStart:" + processEventStart + ",ioreadFinish:" + ioReadEnd);
        }
    }

    //================== biz process =====================

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler",
            method = "processRequest",
            type = "void(org.jboss.netty.channel.ChannelHandlerContext, com.weibo.api.motan.rpc.Request, long)")
    public static void processReqStart(AnyType[] args) {
        traceTag = false;
        if (skipTime()) {
            return;
        }
        if (skipIp(str(get((Map) get(field(classForName("com.weibo.api.motan.rpc.DefaultRequest", contextClassLoader()), "attachments"), args[1]), "host")))) {
            return;
        }
        traceTag = true;
        processReqStart = timeMillis();
        rid = getLong(field(classForName("com.weibo.api.motan.rpc.DefaultRequest", contextClassLoader()), "requestId"), args[1]);
    }

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler",
            method = "processRequest",
            type = "void(org.jboss.netty.channel.ChannelHandlerContext, com.weibo.api.motan.rpc.Request, long)",
            location = @Location(Kind.RETURN))
    public static void processReqEnd(@Duration long time) {
        if (traceTag) {
            processReqEnd = timeMillis();
            println("rid:" + rid + "--type:biz--tid:" + threadId(currentThread()) + "--total:" + (processReqEnd - processReqStart) + "--invoke:" + invokeDuration / 1e6 + ",encode:" + encodeDuration / 1e6 + "--processStart:" + processReqStart + ",invokeStart:" + invokeStart + ",encodeStart:" + encodeStart + ",bizFinish:" + processReqEnd);
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.rpc.DefaultProvider", method = "invoke")
    public static void invokeStart(AnyType[] args) {
        if (traceTag) {
            invokeStart = timeMillis();
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler", method = "processRequest", location = @Location(Kind.RETURN))
    public static void invokeEnd(@Duration long time) {
        if (traceTag) {
            invokeDuration = time;
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.rpc.DefaultProvider", method = "invoke")
    public static void encodeStart(AnyType[] args) {
        if (traceTag) {
            encodeStart = timeMillis();
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.transport.netty.NettyChannelHandler", method = "processRequest", location = @Location(Kind.RETURN))
    public static void encodeEnd(@Duration long time) {
        if (traceTag) {
            encodeDuration = time;
        }
    }

    //================== io write =====================

    @OnMethod(clazz = "org.jboss.netty.channel.socket.nio.NioWorker", method = "write0")
    public static void ioWriteStart(AnyType[] args) {
        traceTag = false;
        if (skipTime()) {
            return;
        }
        if (skipIp(str(get(field(classForName("org.jboss.netty.channel.socket.nio.NioSocketChannel", contextClassLoader()), "remoteAddress"), args[0])))) {
            return;
        }
        traceTag = true;
        ioWriteStart = timeMillis();
    }


    @OnMethod(clazz = "org.jboss.netty.channel.socket.nio.NioWorker", method = "write0", location = @Location(Kind.RETURN))
    public static void ioWriteEnd() {
        if (traceTag) {
            ioWriteEnd = timeMillis();
            println("rid:" + rid + "--type:iowrite--tid:" + threadId(currentThread()) + "--total:" + (ioWriteEnd - ioWriteStart) + "--nativeWrite:" + nativeWriteDuration / 1e6 + "--iowriteStart:" + ioWriteStart + ",nativeStart:" + nativeWriteStart + ",iowriteFinish:" + ioWriteEnd);
        }
    }

    @OnMethod(clazz = "sun.nio.ch.SocketDispatcher", method = "write")
    public static void nativeWrite() {
        if (traceTag) {
            nativeWriteStart = timeMillis();
        }
    }

    @OnMethod(clazz = "sun.nio.ch.SocketDispatcher", method = "write", location = @Location(Kind.RETURN))
    public static void nativeWrite(@Duration long time) {
        if (traceTag) {
            nativeWriteDuration = time;
        }
    }

    @OnMethod(clazz = "com.weibo.api.motan.rpc.AbstractTraceableRequest", method = "onFinish")
    public static void getRid(@Self Object req) {
        if (traceTag) {
            rid = getLong(field(classForName("com.weibo.api.motan.rpc.DefaultRequest", contextClassLoader()), "requestId"), req);
        }
    }


}