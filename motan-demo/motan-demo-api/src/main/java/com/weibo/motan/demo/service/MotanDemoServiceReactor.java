package com.weibo.motan.demo.service;

import com.weibo.motan.demo.service.model.User;
import reactor.core.publisher.Mono;

public interface MotanDemoServiceReactor extends MotanDemoService {
    Mono<String> helloReactor(String name);

    Mono<User> renameReactor(User user, String name);
}
