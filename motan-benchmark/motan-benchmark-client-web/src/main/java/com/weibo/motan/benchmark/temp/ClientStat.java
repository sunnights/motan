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

import java.text.MessageFormat;
import java.util.List;

/**
 * @author sunnights
 */
public class ClientStat {
    private int statisticTime;
    private long above0sum;      // [0,1]
    private long above1sum;      // (1,5]
    private long above5sum;      // (5,10]
    private long above10sum;     // (10,50]
    private long above50sum;     // (50,100]
    private long above100sum;    // (100,500]
    private long above500sum;    // (500,1000]
    private long above1000sum;   // > 1000

    private long maxTPS = 0;
    private long minTPS = 0;
    private long succTPS = 0;
    private long succRT = 0;
    private long errTPS = 0;
    private long errRT = 0;
    private long allTPS = 0;
    private long allRT = 0;
    private long avgTPS = 0;
    private long avgRT = 0;
    private List<RunnableStat> statistics;

    public ClientStat(int statisticTime, List<RunnableStat> statistics) {
        this.statisticTime = statisticTime;
        this.statistics = statistics;
    }

    public int getStatisticTime() {
        return statisticTime;
    }

    public long getAbove0sum() {
        return above0sum;
    }

    public long getAbove1sum() {
        return above1sum;
    }

    public long getAbove5sum() {
        return above5sum;
    }

    public long getAbove10sum() {
        return above10sum;
    }

    public long getAbove50sum() {
        return above50sum;
    }

    public long getAbove100sum() {
        return above100sum;
    }

    public long getAbove500sum() {
        return above500sum;
    }

    public long getAbove1000sum() {
        return above1000sum;
    }

    public long getMaxTPS() {
        return maxTPS;
    }

    public long getMinTPS() {
        return minTPS;
    }

    public long getSuccTPS() {
        return succTPS;
    }

    public long getSuccRT() {
        return succRT;
    }

    public long getErrTPS() {
        return errTPS;
    }

    public long getErrRT() {
        return errRT;
    }

    public long getAllTPS() {
        return allTPS;
    }

    public long getAllRT() {
        return allRT;
    }

    public long getAvgTPS() {
        return avgTPS;
    }

    public long getAvgRT() {
        return avgRT;
    }

    public void collectStatistics() {
        for (RunnableStat statistic : statistics) {
            above0sum += statistic.above0sum;
            above1sum += statistic.above1sum;
            above5sum += statistic.above5sum;
            above10sum += statistic.above10sum;
            above50sum += statistic.above50sum;
            above100sum += statistic.above100sum;
            above500sum += statistic.above500sum;
            above1000sum += statistic.above1000sum;
        }
        for (int i = 0; i < statistics.get(0).statisticTime; i++) {
            long runnableTPS = 0;
            for (RunnableStat statistic : statistics) {
                runnableTPS += (statistic.TPS[i] + statistic.errTPS[i]);
                succTPS += statistic.TPS[i];
                succRT += statistic.RT[i];
                errTPS += statistic.errTPS[i];
                errRT += statistic.errRT[i];
            }
            if (runnableTPS > maxTPS) {
                maxTPS = runnableTPS;
            }
            if (runnableTPS < minTPS || minTPS == 0) {
                minTPS = runnableTPS;
            }
        }
        allTPS = succTPS + errTPS;
        allRT = succRT + errRT;
        avgTPS = allTPS / statisticTime;
        avgRT = allRT / allTPS;
    }

    public void printStatistics() {
        System.out.println("Benchmark Run Time: " + statisticTime);
        System.out.println(MessageFormat.format("Requests: {0}, Success: {1}%({2}), Error: {3}%({4})", allTPS, succTPS * 100 / allTPS, succTPS, errTPS * 100 / allTPS, errTPS));
        System.out.println(MessageFormat.format("Avg TPS: {0}, Max TPS: {1}, Min TPS: {2}", (allTPS / statisticTime), maxTPS, minTPS));
        System.out.println(MessageFormat.format("Avg ResponseTime: {0}ms", allRT / allTPS / 1000f));

        System.out.println(MessageFormat.format("RT [0,1]: {0}% {1}/{2}", above0sum * 100 / allTPS, above0sum, allTPS));
        System.out.println(MessageFormat.format("RT (1,5]: {0}% {1}/{2}", above1sum * 100 / allTPS, above1sum, allTPS));
        System.out.println(MessageFormat.format("RT (5,10]: {0}% {1}/{2}", above5sum * 100 / allTPS, above5sum, allTPS));
        System.out.println(MessageFormat.format("RT (10,50]: {0}% {1}/{2}", above10sum * 100 / allTPS, above10sum, allTPS));
        System.out.println(MessageFormat.format("RT (50,100]: {0}% {1}/{2}", above50sum * 100 / allTPS, above50sum, allTPS));
        System.out.println(MessageFormat.format("RT (100,500]: {0}% {1}/{2}", above100sum * 100 / allTPS, above100sum, allTPS));
        System.out.println(MessageFormat.format("RT (500,1000]: {0}% {1}/{2}", above500sum * 100 / allTPS, above500sum, allTPS));
        System.out.println(MessageFormat.format("RT >1000: {0}% {1}/{2}", above1000sum * 100 / allTPS, above1000sum, allTPS));
    }

}
