package com.weibo.api.motan.transport.netty4.reactor;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.rpc.URL;
import io.netty.bootstrap.Bootstrap;
import io.netty.util.AttributeKey;
import org.reactivestreams.Publisher;
import reactor.netty.NettyOutbound;

import java.util.function.BiFunction;

/**
 * @author sunnights
 */
final class MotanClientConfiguration {
    public static final AttributeKey<MotanClientConfiguration> CONF_KEY = AttributeKey.newInstance("motanClientConf");
    private static final MotanClientConfiguration DEFAULT = new MotanClientConfiguration();
    int minClientConnection = URLParamType.minClientConnection.getIntValue();
    int maxClientConnection = URLParamType.maxClientConnection.getIntValue();
    int maxContentLength = URLParamType.maxContentLength.getIntValue();
    Codec codec;
    URL url;
    BiFunction<MotanClientConnect.MotanClientHandler, ? super NettyOutbound, ? extends Publisher<Void>> body;

    static MotanClientConfiguration getAndClean(Bootstrap b) {
        MotanClientConfiguration mcc = (MotanClientConfiguration) b.config().attrs().get(CONF_KEY);
        b.attr(CONF_KEY, null);
        if (mcc == null) {
            mcc = DEFAULT;
        }
        return mcc;
    }

    static MotanClientConfiguration getOrCreate(Bootstrap b) {
        MotanClientConfiguration mcc = (MotanClientConfiguration) b.config()
                .attrs()
                .get(CONF_KEY);
        if (mcc == null) {
            mcc = new MotanClientConfiguration();
            b.attr(CONF_KEY, mcc);
        }
        return mcc;
    }

    static Bootstrap url(Bootstrap bootstrap, URL url) {
        MotanClientConfiguration configuration = getOrCreate(bootstrap);
        configuration.url = url;
        configuration.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
        configuration.minClientConnection = url.getIntParameter(URLParamType.minClientConnection.getName(), URLParamType.minClientConnection.getIntValue());
        configuration.maxClientConnection = url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());
        configuration.maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(), URLParamType.maxContentLength.getIntValue());
        return bootstrap;
    }

    static Bootstrap body(Bootstrap bootstrap, BiFunction<MotanClientConnect.MotanClientHandler, ? super NettyOutbound, ? extends Publisher<Void>> body) {
        getOrCreate(bootstrap).body = body;
        return bootstrap;
    }
}
