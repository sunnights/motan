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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanglei28 on 2018/6/7.
 */
public class Message {
    Map<Integer, Object> fields = new HashMap<>();

    public Map<Integer, Object> getFields() {
        return fields;
    }

    public void setFields(Map<Integer, Object> fields) {
        this.fields = fields;
    }

    public Object getField(int index) {
        return fields.get(index);
    }

    public void putField(int index, Object field) {
        fields.put(index, field);
    }

}
