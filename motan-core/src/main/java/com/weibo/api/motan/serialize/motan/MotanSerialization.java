/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.serialize.motan;

import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.util.LoggerUtil;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanglei28 on 2018/6/7.
 */
@SpiMeta(name = "motan")
public class MotanSerialization implements Serialization {
    static ConcurrentHashMap<String, MessageTemplate<?>> templates = new ConcurrentHashMap<>();

    @Override
    public byte[] serialize(Object obj) throws IOException {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        return null;
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        return new byte[0];
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        return new Object[0];
    }

    @Override
    public int getSerializationNumber() {
        return 8;
    }

    public static void register(String className, MessageTemplate<?> template) {
        MessageTemplate old = templates.put(className, template);
        if (old != null) {
            LoggerUtil.warn("class template %s already registed in Motanserialization!", className);
        }
    }

    public static MessageTemplate<?> get(String className){
        MessageTemplate template = templates.get(className);
        if (template == null) {
            try {
                template = MyTest.register(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return template;
    }
}
