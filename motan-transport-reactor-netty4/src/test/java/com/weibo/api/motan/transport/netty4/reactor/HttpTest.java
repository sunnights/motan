package com.weibo.api.motan.transport.netty4.reactor;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

public class HttpTest {
    @Test
    public void testReactor() {
        DisposableServer server = HttpServer.create()
                .route(routes -> routes
                        .get("/hello", (request, response) ->
                                response.sendString(Mono.just("Hello World!")))
                        .post("/echo", (request, response) ->
                                response.send(request.receive().retain()))
                        .post("/path/{param}", (request, response) ->
                                response.sendString(Mono.just(request.param("param"))))
                        .ws("/ws", (wsInbound, wsOutbound)
                                -> wsOutbound.send(wsInbound.receive().retain())))
                .bindNow();

        HttpClient httpClient = HttpClient.create().port(server.port());

        Mono<String> s1 = httpClient.post()
                .uri("/echo")
                .send(ByteBufFlux.fromString(Mono.just("hello")))
                .responseContent()
                .aggregate()
                .asString();
//        System.out.println(s1.block());
        Mono<String> s2 = httpClient.post()
                .uri("/echo")
                .send(ByteBufFlux.fromString(s1.map(s -> s + ", test")))
                .responseContent()
                .aggregate()
                .asString();
        System.out.println(s2.block());
        server.disposeNow();
    }
}
