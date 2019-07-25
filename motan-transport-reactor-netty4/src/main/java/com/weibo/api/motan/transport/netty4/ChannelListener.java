package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.rpc.Response;

/**
 * @author sunnights
 */
public interface ChannelListener {
    void onStateChange(Response response);
}
