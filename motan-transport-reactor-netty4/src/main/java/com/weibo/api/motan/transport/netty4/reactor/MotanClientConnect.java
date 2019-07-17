package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.rpc.URL;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.netty.*;
import reactor.netty.channel.BootstrapHandlers;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.context.Context;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author sunnights
 */
public class MotanClientConnect extends MotanClient {
    private static final Logger log = Loggers.getLogger(MotanClientConnect.class);
    private final MotanTcpClient defaultClient;

    public MotanClientConnect(TcpClient tcpClient) {
        Objects.requireNonNull(tcpClient, "tcpClient");
        defaultClient = new MotanTcpClient(tcpClient);
    }

    @Override
    protected TcpClient tcpConfiguration() {
        return defaultClient;
    }

    static final class MotanTcpClient extends TcpClient {
        final TcpClient defaultClient;

        public MotanTcpClient(TcpClient tcpClient) {
            defaultClient = tcpClient;
        }

        @Override
        public Mono<? extends Connection> connect(Bootstrap b) {
            if (b.config().group() == null) {
                LoopResources loopResources = MotanResources.get();
                EventLoopGroup eventLoopGroup = loopResources.onClient(false);
                Integer maxConnections = (Integer) b.config().attrs().get(AttributeKey.valueOf("maxConnections"));
                if (maxConnections != null && maxConnections != -1 && eventLoopGroup instanceof Supplier) {
                    EventLoopGroup delegate = (EventLoopGroup) ((Supplier) eventLoopGroup).get();
                    b.group(delegate)
                            .channel(loopResources.onChannel(delegate));
                } else {
                    b.group(eventLoopGroup)
                            .channel(loopResources.onChannel(eventLoopGroup));
                }
            }
            MotanClientConfiguration conf = MotanClientConfiguration.getAndClean(b);
            BootstrapHandlers.channelOperationFactory(b, (ch, c, msg) -> new MotanClientOperations(ch, c, conf.codec, conf.url));

            return new MonoMotanConnect(b, conf, defaultClient);
        }

        @Override
        public Bootstrap configure() {
            return defaultClient.configure();
        }
    }

    static final class MonoMotanConnect extends Mono<Connection> {
        final Bootstrap bootstrap;
        final MotanClientConfiguration configuration;
        final TcpClient tcpClient;

        public MonoMotanConnect(Bootstrap bootstrap, MotanClientConfiguration configuration, TcpClient tcpClient) {
            this.bootstrap = bootstrap;
            this.configuration = configuration;
            this.tcpClient = tcpClient;
        }

        @Override
        public void subscribe(CoreSubscriber<? super Connection> actual) {
            final Bootstrap b = bootstrap.clone();
            MotanClientHandler handler = new MotanClientHandler(configuration, b.config().remoteAddress());
            b.remoteAddress(handler);
            BootstrapHandlers.updateConfiguration(b, NettyPipeline.LEFT + "motanInitializer", new MotanInitializer(handler));
            Mono.<Connection>create(sink -> {
                Bootstrap finalBootstrap = b.clone();
                BootstrapHandlers.connectionObserver(finalBootstrap,
                        new MotanObserver(sink, handler)
                                .then(BootstrapHandlers.connectionObserver(finalBootstrap))
                                .then(new MotanIOHandlerObserver(sink, handler))
                );
                tcpClient.connect(finalBootstrap)
                        .subscribe(new TcpClientSubscriber(sink));
            }).retry(handler)
                    .subscribe(actual);
        }

        private class TcpClientSubscriber implements CoreSubscriber<Connection> {
            final MonoSink<Connection> sink;

            public TcpClientSubscriber(MonoSink<Connection> sink) {
                this.sink = sink;
            }

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Connection connection) {
                sink.onCancel(connection);
            }

            @Override
            public void onError(Throwable t) {
                sink.error(t);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public Context currentContext() {
                return sink.currentContext();
            }
        }
    }

    final static class MotanObserver implements ConnectionObserver {
        final MonoSink<Connection> sink;
        final MotanClientHandler handler;

        public MotanObserver(MonoSink<Connection> sink, MotanClientHandler handler) {
            this.sink = sink;
            this.handler = handler;
        }

        @Override
        public Context currentContext() {
            return sink.currentContext();
        }

        @Override
        public void onStateChange(Connection connection, State newState) {
            if (newState == MotanClientState.RESPONSE_RECEIVED) {
                sink.success(connection);
                return;
            }
            if (newState == State.CONFIGURED && MotanClientOperations.class == connection.getClass()) {
                handler.channel((MotanClientOperations) connection);
            }
        }

        @Override
        public void onUncaughtException(Connection connection, Throwable error) {
            // todo
            sink.error(error);
        }
    }

    static final class MotanIOHandlerObserver implements ConnectionObserver {
        final MonoSink<Connection> sink;
        final MotanClientHandler handler;

        public MotanIOHandlerObserver(MonoSink<Connection> sink, MotanClientHandler handler) {
            this.sink = sink;
            this.handler = handler;
        }

        @Override
        public Context currentContext() {
            return sink.currentContext();
        }

        @Override
        public void onStateChange(Connection connection, State newState) {
            if (newState == State.CONFIGURED && MotanClientOperations.class == connection.getClass()) {
                log.debug(ReactorNetty.format(connection.channel(), "Handler is being applied: {}"), handler);
                Mono.defer(() -> Mono.fromDirect(handler.request((MotanClientOperations) connection)))
                        .subscribe(connection.disposeSubscriber());
            }
        }
    }

    static final class MotanClientHandler extends SocketAddress implements Predicate<Throwable>, Supplier<SocketAddress> {
        final MotanClientConfiguration configuration;
        final SocketAddress socketAddress;
        final int minClientConnection;
        final int maxClientConnection;
        final int maxContentLength;
        final Codec codec;
        final URL url;
        final BiFunction<MotanClientHandler, ? super NettyOutbound, ? extends Publisher<Void>> handler;

        public MotanClientHandler(MotanClientConfiguration configuration, SocketAddress socketAddress) {
            this.configuration = configuration;
            this.socketAddress = socketAddress;
            minClientConnection = configuration.minClientConnection;
            maxClientConnection = configuration.maxClientConnection;
            maxContentLength = configuration.maxContentLength;
            codec = configuration.codec;
            url = configuration.url;
            handler = configuration.body;
        }

        @Override
        public boolean test(Throwable throwable) {
            return false;
        }

        @Override
        public SocketAddress get() {
            return socketAddress;
        }

        public void channel(MotanClientOperations operations) {
        }

        public Publisher<Void> request(MotanClientOperations operations) {
            if (handler != null) {
                return handler.apply(this, operations);
            }
            // todo 没有配置handler时调用
            return operations.send();
        }
    }

    static final class MotanInitializer extends ChannelInboundHandlerAdapter implements BiConsumer<ConnectionObserver, Channel>, ChannelOperations.OnSetup {
        final MotanClientHandler handler;

        public MotanInitializer(MotanClientHandler handler) {
            this.handler = handler;
        }

        @Override
        public void accept(ConnectionObserver connectionObserver, Channel channel) {
            channel.pipeline()
                    .addLast(NettyPipeline.LEFT + "motanDecoder", new NettyDecoder(handler.maxContentLength))
                    .addLast(NettyPipeline.LEFT + "motanEncoder", new NettyEncoder(handler.codec, handler.url));
        }

        @Override
        public ChannelOperations<?, ?> create(Connection c, ConnectionObserver listener, Object msg) {
            return new MotanClientOperations(c, listener, handler.codec, handler.url);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("--- channelRegistered");
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("--- channelUnregistered");
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ChannelOperations<?, ?> ops = ChannelOperations.get(ctx.channel());
            if (ops != null) {
                ops.listener().onStateChange(ops, ConnectionObserver.State.CONFIGURED);
            }
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("--- channelInactive");
            super.channelInactive(ctx);
        }
    }
}
