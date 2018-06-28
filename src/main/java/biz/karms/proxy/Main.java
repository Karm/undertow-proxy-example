/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package biz.karms.proxy;

import io.undertow.Undertow;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See: https://github.com/undertow-io/undertow/tree/master/examples/src/main/java/io/undertow/examples/reverseproxy
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger("Proxy");

    public static void main(final String[] args) throws URISyntaxException {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        if (args.length < 3 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            LOGGER.log(Level.INFO,"Usage: <proxy address> <proxy port> [<schema://worker address:worker port> .. ]\n\te.g. java -jar undertowproxy.jar 192.168.122.172 2080 http://192.168.122.172:8080 http://192.168.122.172:8081 http://192.168.122.172:8082");
            System.exit(0);
        }
        final LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                .setConnectionsPerThread(20);
        for (String worker : Arrays.asList(args).subList(2, args.length)) {
            LOGGER.log(Level.INFO, String.format("Adding worker %s", worker));
            loadBalancer.addHost(new URI(worker));
        }
        final int port = Integer.parseInt(args[1]);
        LOGGER.log(Level.INFO, String.format("Creating proxy on %s:%d", args[0], port));
        final Undertow reverseProxy = Undertow.builder()
                .addHttpListener(port, args[0])
                .setIoThreads(4)
                .setHandler(ProxyHandler.builder().setProxyClient(loadBalancer).setMaxRequestTime(30000).build())
                .build();
        reverseProxy.start();
    }
}
