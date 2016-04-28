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

package com.weibo.api.motan.registry.zookeeper;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZookeeperRegistryTest {
    private ZookeeperRegistry registry;
    private ZkClient zkClient;
    private URL clientUrl;
    private URL url1;
    private URL url2;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        // zookeeper://127.0.0.1:2181/com.weibo.api.motan.registry.RegistryService?group=yf_rpc
        URL zkUrl = new URL("zookeeper", "127.0.0.1", port, "com.weibo.api.motan.registry.RegistryService");
        clientUrl = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 0, "com.weibo.motan.demoService");
        url1 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demoService");
        url2 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8002, "com.weibo.motan.demoService");

        EmbeddedZookeeper embeddedZookeeper = new EmbeddedZookeeper();
        embeddedZookeeper.start();

        zkClient = new ZkClient("127.0.0.1:" + port);
        registry = new ZookeeperRegistry(zkUrl, zkClient);
    }

    @After
    public void tearDown() throws Exception {
        zkClient.deleteRecursive(MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Test
    public void doRegister() throws Exception {
        registry.doRegister(url1);

        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));
    }

    @Test
    public void doUnregister() throws Exception {
        registry.doUnregister(url1);

        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
    }

    @Test
    public void doSubscribe() throws Exception {
        registry.doRegister(url1);
        registry.doAvailable(url1);

        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(URL registryUrl, List<URL> urls) {
            }
        };
        registry.doSubscribe(clientUrl, notifyListener);

        ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = registry.getUrlListeners();
        assertTrue(urlListeners.containsKey(clientUrl));
        assertTrue(urlListeners.get(clientUrl).get(notifyListener) != null);
        assertTrue(zkClient.exists(registry.toNodePath(clientUrl, ZkNodeType.CLIENT)));
    }

    @Test
    public void doUnsubscribe() throws Exception {
        registry.register(url1);
        registry.doAvailable(url1);

        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(URL registryUrl, List<URL> urls) {
            }
        };
        ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = registry.getUrlListeners();
        registry.doSubscribe(clientUrl, notifyListener);
        registry.doUnsubscribe(clientUrl, notifyListener);

        assertTrue(urlListeners.get(clientUrl).isEmpty());
    }

    @Test
    public void doDiscover() throws Exception {
        registry.doRegister(url1);
        registry.doAvailable(url1);
        List<URL> urls = registry.doDiscover(url1);

        assertTrue(urls.contains(url1));
    }

    @Test
    public void doAvailable() throws Exception {
        final Set<URL> urls = new HashSet<URL>();
        urls.add(url1);
        urls.add(url2);
        for (URL u : urls) {
            registry.register(u);
        }

        registry.available(url1);
        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));

        registry.available(null);
        for (URL u : urls) {
            assertTrue(zkClient.exists(registry.toNodePath(u, ZkNodeType.AVAILABLE_SERVER)));
            assertFalse(zkClient.exists(registry.toNodePath(u, ZkNodeType.UNAVAILABLE_SERVER)));
        }
    }

    @Test
    public void doUnavailable() throws Exception {
        Set<URL> urls = new HashSet<URL>();
        urls.add(url1);
        urls.add(url2);
        for (URL u : urls) {
            registry.register(u);
        }

        registry.unavailable(url1);
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));

        registry.unavailable(null);
        for (URL u : urls) {
            assertFalse(zkClient.exists(registry.toNodePath(u, ZkNodeType.AVAILABLE_SERVER)));
            assertTrue(zkClient.exists(registry.toNodePath(u, ZkNodeType.UNAVAILABLE_SERVER)));
        }
    }

    @Test
    public void createNode() throws Exception {
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));

        registry.createNode(url1, ZkNodeType.AVAILABLE_SERVER);
        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
    }

    @Test
    public void removeNode() throws Exception {
        registry.createNode(url1, ZkNodeType.AVAILABLE_SERVER);
        registry.removeNode(url1, ZkNodeType.AVAILABLE_SERVER);
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
    }

}