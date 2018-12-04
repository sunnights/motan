package com.weibo;

import cn.sina.api.commons.thead.RequestTraceContext;
import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.data.service.FriendService;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.util.MotanSwitcherUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserServiceTest {
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    public static final String usergraph1_tc = "10.22.6.224,10.22.6.226,10.22.6.241,10.22.6.247,10.22.6.250,10.22.6.94,10.22.6.97,10.22.6.99,10.22.7.16,10.39.56.56,10.39.56.84,10.39.60.26,10.39.60.37,10.39.60.40,10.39.60.45,10.39.60.46,10.73.14.213,10.73.14.30,10.73.32.229,10.73.88.176,10.73.88.177,10.73.88.178,10.73.88.179,10.73.88.180,10.73.88.181,10.73.88.182,10.73.89.23,10.73.89.29,10.73.89.30,10.73.89.31,10.73.89.32,10.73.89.33,10.73.89.35,10.73.89.65,10.73.89.66,10.73.89.78,10.73.89.79,10.73.89.80,10.77.29.210";
    public static final String usergraph1_tc_core = "10.22.6.151,10.22.6.164,10.22.6.165,10.22.6.168,10.22.6.177,10.22.6.186,10.22.6.187,10.22.6.190,10.22.6.191,10.73.89.111,10.73.89.119,10.73.89.122,10.73.89.124,10.73.89.25,10.73.89.26,10.73.89.27,10.73.89.51,10.73.89.52,10.73.89.53,10.73.89.54,10.73.89.55,10.73.89.56,10.73.89.57,10.73.89.58,10.73.89.59,10.73.89.61,10.73.89.62,10.73.89.63,10.73.89.64,10.73.89.77";

    public static void main(String[] args) throws InterruptedException {
        final List<FriendService> serviceList = new ArrayList<>();
        for (String ip : usergraph1_tc.split(",")) {
            serviceList.add(buildService(ip + ":8882", false));
        }
        for (String ip : usergraph1_tc_core.split(",")) {
            serviceList.add(buildService(ip + ":8882", true));
        }
        MotanSwitcherUtil.initSwitcher(MotanConstants.REQUEST_TRACK_LOG_SWITCHER, true);

        EXECUTOR_SERVICE.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (FriendService friendService : serviceList) {
                    callService(friendService);
                }
            }
        }, 1000, 10, TimeUnit.MILLISECONDS);

        /*for (int i = 0; i < 1; i++) {
            for (FriendService friendService : serviceList) {
                callService(friendService);
            }
            Thread.sleep(1000);
        }*/
        Thread.sleep(Integer.MAX_VALUE);
        System.exit(0);
    }

    public static FriendService buildService(String directIp, boolean core) {
        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress(directIp);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan");
        protocol.setName("motan");
        protocol.setHaStrategy("failfast");
        protocol.setLoadbalance("roundrobin");

        RefererConfig<FriendService> referer = new RefererConfig<>();
        // 设置接口及实现类
        referer.setInterface(FriendService.class);
        referer.setRegistry(registry);
        referer.setProtocol(protocol);
        referer.setApplication("user-test");
        referer.setModule("user-test");
        if (core) {
            referer.setGroup("tc-rpc-core");
        } else {
            referer.setGroup("tc-rpc");
        }
        referer.setRequestTimeout(500);
        referer.setRetries(0);
        referer.setAccessLog("true");
        referer.setThrowException(true);
        return referer.getRef();
    }

    public static void callService(FriendService friendService) {
        try {
            RequestTraceContext.init();
            friendService.getRemarks(1787213787L, new long[]{1822712071L});
        } catch (Exception e) {
            ApiLogger.error(e.getMessage());
        } finally {
            RequestTraceContext.clear();
        }
    }
}
