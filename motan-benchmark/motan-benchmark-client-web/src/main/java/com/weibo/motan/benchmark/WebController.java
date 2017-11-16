package com.weibo.motan.benchmark;

import com.github.abel533.echarts.Option;
import com.github.abel533.echarts.axis.AxisLabel;
import com.github.abel533.echarts.axis.CategoryAxis;
import com.github.abel533.echarts.axis.ValueAxis;
import com.github.abel533.echarts.code.*;
import com.github.abel533.echarts.feature.MagicType;
import com.github.abel533.echarts.series.Bar;
import com.github.abel533.echarts.series.Line;
import com.github.abel533.echarts.series.Series;
import com.github.abel533.echarts.style.ItemStyle;
import com.github.abel533.echarts.style.itemstyle.Normal;
import com.google.gson.Gson;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.motan.benchmark.temp.ClientStat;
import com.weibo.motan.benchmark.temp.MotanBenchmarkClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sunnights
 */
@RestController
public class WebController {

    @RequestMapping("/benchmark.json")
    public String benchmark(HttpServletRequest request) throws ClassNotFoundException, InterruptedException {
        BenchmarkService benchmarkService = getBenchmarkService(request);
        List<Integer> concurrentList = Arrays.stream(request.getParameter("concurrent").split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        List<String> categoryList = Arrays.asList(request.getParameter("category").split(","));
        int warmupTime = Integer.parseInt(request.getParameter("warmupTime"));
        int benchmarkTime = Integer.parseInt(request.getParameter("benchmarkTime"));
        Map<String, Map<String, Long>> tpsData = new LinkedHashMap<>(concurrentList.size());
        Map<String, Map<String, Long>> rtData = new LinkedHashMap<>(concurrentList.size());
        for (String category : categoryList) {
            String classname = null;
            String str = "";
            switch (category) {
                case "Empty":
                    classname = "com.weibo.motan.benchmark.temp.impl.TestEmptyRunnable";
                    break;
                case "POJO":
                    classname = "com.weibo.motan.benchmark.temp.impl.TestPojoRunnable";
                    break;
                default:
                    if (!category.contains("KString")) {
                        break;
                    }
                    classname = "com.weibo.motan.benchmark.temp.impl.TestStringRunnable";
                    String size = category.split("KString")[0];
                    int length = 1024 * Integer.parseInt(size);
                    StringBuilder builder = new StringBuilder(length);
                    for (int i = 0; i < length; i++) {
                        builder.append((char) (ThreadLocalRandom.current().nextInt(33, 128)));
                    }
                    str = builder.toString();
                    break;
            }
            if (StringUtils.isBlank(classname)) {
                continue;
            }

            for (int num : concurrentList) {
                ClientStat stat = new MotanBenchmarkClient(benchmarkService).start(num, warmupTime, benchmarkTime, classname, str);
                String name = num + "-并发";

                long avgTPS = stat.getAvgTPS();
                Map<String, Long> tpsItem = tpsData.get(name);
                if (tpsItem == null) {
                    tpsItem = new LinkedHashMap<>();
                }
                tpsItem.put(category, avgTPS);
                tpsData.put(name, tpsItem);

                long avgRT = stat.getAvgRT();
                Map<String, Long> rtItem = rtData.get(name);
                if (rtItem == null) {
                    rtItem = new LinkedHashMap<>();
                }
                rtItem.put(category, avgRT);
                rtData.put(name, rtItem);

                System.gc();
                Thread.sleep(5000);
            }
        }

        return draw(tpsData, rtData);
    }

    public BenchmarkService getBenchmarkService(HttpServletRequest request) throws ClassNotFoundException {
        RegistryConfig directRegistryConfig = new RegistryConfig();
        directRegistryConfig.setRegProtocol("local");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setDefault(true);
        protocolConfig.setName(request.getParameter("protocolName"));
        protocolConfig.setSerialization(request.getParameter("serialization"));
        protocolConfig.setEndpointFactory(request.getParameter("endpointFactory"));
        protocolConfig.setHaStrategy(request.getParameter("haStrategy"));
        protocolConfig.setLoadbalance(request.getParameter("loadBalance"));
        protocolConfig.setMinClientConnection(Integer.valueOf(request.getParameter("minConnection")));
        protocolConfig.setMaxClientConnection(Integer.valueOf(request.getParameter("maxConnection")));

        RefererConfig<BenchmarkService> refererConfig = new RefererConfig<>();
        refererConfig.setInterface(BenchmarkService.class);
        refererConfig.setGroup(request.getParameter("group"));
        refererConfig.setRequestTimeout(Integer.valueOf(request.getParameter("requestTimeout")));
        refererConfig.setModule(request.getParameter("module"));
        refererConfig.setApplication(request.getParameter("application"));
        refererConfig.setRetries(Integer.valueOf(request.getParameter("retries")));
        refererConfig.setThrowException(Boolean.valueOf(request.getParameter("throwException")));
        refererConfig.setDirectUrl(request.getParameter("directUrl"));
        refererConfig.setRegistry(directRegistryConfig);
        refererConfig.setProtocol(protocolConfig);

        return refererConfig.getRef();
    }

    public String draw(Map<String, Map<String, Long>> tpsData, Map<String, Map<String, Long>> rtData) {
        Option option = new Option();
        option.tooltip().trigger(Trigger.axis)
                .axisPointer()
                .type(PointerType.cross);
        option.toolbox()
                .show(true)
                .feature(
                        Tool.dataView,
                        new MagicType(Magic.bar, Magic.line),
                        Tool.restore,
                        Tool.saveAsImage
                );

        Set<String> legend = new LinkedHashSet<>();
        legend.addAll(Stream.of(tpsData, rtData)
                .flatMap(data -> data.keySet().stream())
                .collect(Collectors.toList()));
        option.legend(legend.toArray());

        Set<String> category = new LinkedHashSet<>();
        category.addAll(Stream.of(tpsData, rtData)
                .flatMap(data -> data.values().stream())
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toList()));
        option.xAxis(new CategoryAxis().data(category.toArray()));

        option.yAxis(new ValueAxis().type(AxisType.value).name("TPS").axisLabel(new AxisLabel().formatter("{value} TPS")),
                new ValueAxis().type(AxisType.value).name("RT").axisLabel(new AxisLabel().formatter("{value} ms")));

        ItemStyle itemStyle = new ItemStyle().normal(new Normal().show(true).position(Position.top));
        List<Series> series = new ArrayList<>();
        tpsData.forEach((key, value) -> series.add(new Bar(key)
                .barGap("0")
                .label(itemStyle)
                .data(value.values().toArray())));
        rtData.forEach((key, value) -> series.add(new Line(key)
                .yAxisIndex(1)
                .data(value.values().stream().map(rt -> rt / 1000f).collect(Collectors.toList()).toArray())));
        option.series(series);

        String result = new Gson().toJson(option);
        System.out.println(result);
        return result;
    }
}
