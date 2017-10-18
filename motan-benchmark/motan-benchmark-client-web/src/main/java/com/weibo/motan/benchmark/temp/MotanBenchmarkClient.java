/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.motan.benchmark.temp;

import com.weibo.motan.benchmark.BenchmarkService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class MotanBenchmarkClient extends AbstractBenchmarkClient {

    private BenchmarkService benchmarkService;

    public MotanBenchmarkClient(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @Override
    public ClientRunnable getClientRunnable(String classname, String size, CyclicBarrier barrier,
                                            CountDownLatch latch, long startTime, long endTime) {
        Class[] parameterTypes = new Class[]{BenchmarkService.class, String.class, CyclicBarrier.class,
                CountDownLatch.class, long.class, long.class};
        Object[] parameters = new Object[]{benchmarkService, size, barrier, latch, startTime, endTime};

        ClientRunnable clientRunnable = null;
        try {
            clientRunnable = (ClientRunnable) Class.forName(classname).getConstructor(parameterTypes).newInstance(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return clientRunnable;
    }
}
