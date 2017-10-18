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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractBenchmarkClient {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private int concurrent;
    private int benchmarkTime;
    private String classname;
    private String size;
    private ClientStat statistics;

    /**
     * @param concurrent    并发线程数
     * @param warmupTime    预热时间
     * @param benchmarkTime 性能测试时间
     * @param classname     测试的类名
     * @param size          测试String时，指String的size，单位为k
     */
    public ClientStat start(int concurrent, int warmupTime, int benchmarkTime, String classname, String size) {
        this.concurrent = concurrent;
        this.benchmarkTime = benchmarkTime;
        this.classname = classname;
        this.size = size;

        printStartInfo();

        // prepare runnables
        long currentTime = System.nanoTime() / 1000L;
        long startTime = currentTime + warmupTime * 1000 * 1000L;
        long endTime = startTime + this.benchmarkTime * 1000 * 1000L;

        List<ClientRunnable> runnables = new ArrayList<>();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(this.concurrent);
        CountDownLatch countDownLatch = new CountDownLatch(this.concurrent);
        for (int i = 0; i < this.concurrent; i++) {
            ClientRunnable runnable = getClientRunnable(classname, size, cyclicBarrier, countDownLatch, startTime, endTime);
            runnables.add(runnable);
            Thread thread = new Thread(runnable, "benchmark-client-" + i);
            thread.start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<RunnableStat> runnableStatList = new ArrayList<>();
        for (ClientRunnable runnable : runnables) {
            runnableStatList.add(runnable.getStatistics());
        }
        statistics = new ClientStat(this.benchmarkTime, runnableStatList);
        statistics.collectStatistics();

        printStatistics();
        return statistics;
    }

    private void printStartInfo() {
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.SECOND, benchmarkTime);
        Date finishDate = calendar.getTime();

        String startInfo = dateFormat.format(currentDate) + " ready to start client benchmark" +
                ", concurrent num is " + concurrent +
                ", the benchmark will end at " + dateFormat.format(finishDate);

        System.out.println(startInfo);
    }

    private void printStatistics() {
        System.out.println("----------Benchmark Statistics--------------");
        System.out.println("Concurrent: " + concurrent);
        System.out.println("Runtime: " + benchmarkTime + " seconds");
        System.out.println("ClassName: " + classname);
        System.out.println("Size: " + size);
        statistics.printStatistics();
    }

    public abstract ClientRunnable getClientRunnable(String classname, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime);
}
