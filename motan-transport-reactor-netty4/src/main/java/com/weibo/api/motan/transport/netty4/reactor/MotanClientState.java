package com.weibo.api.motan.transport.netty4.reactor;

import reactor.netty.ConnectionObserver;

/**
 * @author sunnights
 */
public enum MotanClientState implements ConnectionObserver.State {
    /**
     * The request has been sent
     */
    REQUEST_SENT() {
        @Override
        public String toString() {
            return "[request_sent]";
        }
    },
    /**
     * The request has been sent but the response has not been fully received and the
     * connection has prematurely closed
     */
    RESPONSE_INCOMPLETE() {
        @Override
        public String toString() {
            return "[response_incomplete]";
        }
    },
    /**
     * The response status and headers have been received
     */
    RESPONSE_RECEIVED() {
        @Override
        public String toString() {
            return "[response_received]";
        }
    }
}
