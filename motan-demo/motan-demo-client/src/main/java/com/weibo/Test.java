package com.weibo;

import cn.sina.api.commons.thead.RequestTraceContext;
import cn.sina.api.commons.util.ApiLogger;
import cn.sina.api.user.service.UserMappingService;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Test {
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:rpc-client.xml");
        MotanSwitcherUtil.initSwitcher(MotanConstants.REQUEST_TRACK_LOG_SWITCHER, true);
        final UserMappingService userMappingService = ctx.getBean("userMappingService", UserMappingService.class);

        /*EXECUTOR_SERVICE.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestTraceContext.init();
                    userMappingService.getUserTypes(Arrays.asList(Long.parseLong("6050" + RandomUtils.nextInt(100000, 999999))));
                } catch (Exception e) {
                    ApiLogger.error(e.getMessage(), e);
                } finally {
                    RequestTraceContext.clear();
                }
            }
        }, 1000, 10, TimeUnit.MILLISECONDS);*/


        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                RequestTraceContext.init();
//                userMappingService.getUserTypes(Arrays.asList(Long.parseLong("6050" + RandomUtils.nextInt(100000, 999999))));
                userMappingService.getUserTypes(Arrays.asList(2522481887L));
            } catch (Exception e) {
                ApiLogger.error(e.getMessage(), e);
            } finally {
                RequestTraceContext.clear();
            }
            Thread.sleep(1000);
        }
        System.exit(0);
    }

}
