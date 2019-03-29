/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.net.internal.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.net.internal.config.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

/**
 * The {@link UdpServerHandler} is responsible for handling UDP server thing functionality.
 *
 * @author Pauli Anttila - Initial contribution
 *
 */
public class UdpServerHandler extends AbstractServerBridge {

    private final Logger logger = LoggerFactory.getLogger(UdpServerHandler.class);

    private ServerConfiguration configuration;
    private Connection server;

    public UdpServerHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Unsupported command '{}' received for channel '{}'", command, channelUID);
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(ServerConfiguration.class);
        logger.debug("Using configuration: {}", configuration);

        try {
            startServer();
            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.debug("Exception occurred during initalization: {}. ", e.getMessage(), e);
            shutdownServer();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Stopping thing");
        shutdownServer();
    }

    private void startServer() {
        logger.debug("Start UDP server");

        server = UdpServer.create().port(configuration.port).handle((in, out) -> {
            in.receive().asByteArray().subscribe(bytes -> {
                sendData(configuration.convertTo, bytes);
            });
            return Flux.never();
        }).bind().block();
        logger.debug("UDP server started");
    }

    private void shutdownServer() {
        logger.debug("Shutdown UDP server");
        if (server != null) {
            server.disposeNow();
        }
        logger.debug("UDP server stopped");
    }
}