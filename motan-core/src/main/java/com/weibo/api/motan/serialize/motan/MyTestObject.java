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

import java.util.List;
import java.util.Map;

/**
 * Created by zhanglei28 on 2018/6/7.
 */
public class MyTestObject {
    public String f3;
    int f1;
    float f2;
    Map<String, String> f4;
    MyTestSubObject f5;
    List<Object> f6;

    public int getF1() {
        return f1;
    }

    public void setF1(int f1) {
        this.f1 = f1;
    }

    public float getF2() {
        return f2;
    }

    public void setF2(float f2) {
        this.f2 = f2;
    }

    public String getF3() {
        return f3;
    }

    public void setF3(String f3) {
        this.f3 = f3;
    }

    public Map<String, String> getF4() {
        return f4;
    }

    public void setF4(Map<String, String> f4) {
        this.f4 = f4;
    }

    public MyTestSubObject getF5() {
        return f5;
    }

    public void setF5(MyTestSubObject f5) {
        this.f5 = f5;
    }

    public List<Object> getF6() {
        return f6;
    }

    public void setF6(List<Object> f6) {
        this.f6 = f6;
    }

    public static class MyTestSubObject {
        String f1;
        List<String> f2;

        public String getF1() {
            return f1;
        }

        public void setF1(String f1) {
            this.f1 = f1;
        }

        public List<String> getF2() {
            return f2;
        }

        public void setF2(List<String> f2) {
            this.f2 = f2;
        }

    }


}
