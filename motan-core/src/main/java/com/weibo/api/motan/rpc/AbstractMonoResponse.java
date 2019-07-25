package com.weibo.api.motan.rpc;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author sunnights
 */
public abstract class AbstractMonoResponse extends Mono<Response> implements Response {

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public long getRequestId() {
        return 0;
    }

    @Override
    public long getProcessTime() {
        return 0;
    }

    @Override
    public void setProcessTime(long time) {

    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public Map<String, String> getAttachments() {
        return null;
    }

    @Override
    public void setAttachment(String key, String value) {

    }

    @Override
    public byte getRpcProtocolVersion() {
        return 0;
    }

    @Override
    public void setRpcProtocolVersion(byte rpcProtocolVersion) {

    }
}
