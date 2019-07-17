package com.weibo.api.motan.transport.netty4.reactor;

import com.alibaba.fastjson.JSON;
import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.netty4.NettyDecoder;
import com.weibo.api.motan.transport.netty4.NettyEncoder;
import com.weibo.api.motan.transport.netty4.*;
import com.weibo.api.motan.util.RequestIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sunnights
 */
public class MotanClientTests {
    private NettyServer nettyServer;
    private DefaultRequest request;
    private URL url;
    private String interfaceName = "com.weibo.api.motan.protocol.example.IHello";

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, interfaceName, parameters);

        request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName("hello");
        request.setParamtersDesc("void");
//        System.out.println(JSON.toJSONString(request));

        nettyServer = new NettyServer(url, (channel, message) -> {
            Request request = (Request) message;
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());
//            System.out.println(JSON.toJSONString(request));
            System.out.println("[Server] id:" + request.getRequestId());
            return response;
        });

        nettyServer.open();
    }

    @After
    public void tearDown() throws Exception {
        nettyServer.close();
    }

    @Test
    public void testClient() throws InterruptedException {
        MotanClient.RequestSender client = MotanClient.create()
                .url(url)
                .build();
        callRequest(client, request);
        Thread.sleep(3000);
        callRequest(client, request);
        Thread.sleep(10000);
    }

    private void callRequest(MotanClient.RequestSender client, DefaultRequest request) {
        request.setRequestId(RequestIdGenerator.getRequestId());
        Mono<Response> content = client.send(Mono.just(request))
                .response();
        Response response = content.block();
        System.out.println(client);
        System.out.println(JSON.toJSONString(response));
        System.out.println("---");
    }

    @Test
    public void testTcpClient() throws InterruptedException {
        Codec codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
        int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(), URLParamType.maxContentLength.getIntValue());
        TcpClient client = TcpClient.create()
                .host(url.getHost())
                .port(url.getPort())
                .doOnConnected(c -> c.addHandlerLast("decoder", new NettyDecoder(codec, new SimpleChannel(url), maxContentLength))
                        .addHandlerLast("encoder", new NettyEncoder()));

        callRequest(client, request);
        request.setRequestId(RequestIdGenerator.getRequestId());
        callRequest(client, request);
    }

    private void callRequest(TcpClient client, Request request) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Codec codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
        byte[] msg = CodecUtil.encodeObjectToBytes(new SimpleChannel(url), codec, request);
        Connection connection = client.handle((in, out) -> {
            in.receiveObject()
                    .cast(NettyMessage.class)
                    .doOnNext(nettyMessage -> System.out.println(JSON.toJSONString(nettyMessage)))
                    .subscribe(nettyMessage -> latch.countDown());
            return out.sendByteArray(Mono.just(msg))
                    .neverComplete();
        })
                .connectNow();
        System.out.println(connection.toString());
        System.out.println(client);
        Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
        connection.disposeNow();
        System.out.println(connection.toString());
        System.out.println(client);
        System.out.println("---");
    }
}