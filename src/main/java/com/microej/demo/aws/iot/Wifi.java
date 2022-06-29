/*
 * Java
 *
 * Copyright 2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.me.JSONException;

import com.microej.example.wifi.setup.ConnectorListener;
import com.microej.example.wifi.setup.web.WebSoftAPConnector;

import ej.aws.iot.AwsIotException;
import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SoftAPConfiguration;
import ej.net.util.NetUtil;
import ej.net.util.NtpUtil;
import ej.net.util.wifi.AccessPointConfiguration;

/**
 * WIFI State Listener
 */
@SuppressWarnings("nls")
public class Wifi implements ConnectorListener {

	private static final Logger LOGGER = Logger.getLogger(Wifi.class.getName());

	private final WebSoftAPConnector connector;

	/**
	 *
	 * @param connector
	 *            websoft ap connector
	 */
	public Wifi(final WebSoftAPConnector connector) {
		this.connector = connector;
	}

	@Override
	public void onSoftAPMount(SoftAPConfiguration softAPConfiguration) {
		// Wait for an IP address
		while (NetUtil.getFirstHostAddress() == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// no-op
			}
		}
		final String host = NetUtil.getFirstHostAddress().getHostAddress();
		final int port = this.connector.getServerPort();
		LOGGER.info("\n############ Wi-Fi CONFIG INSTRUCTIONS ###################\n" //
				+ "Please connect to the Wi-Fi network with SSID=" + softAPConfiguration.getSSID() //
				+ ", password=" + softAPConfiguration.getPassphrase() //
				+ ", and security=" + softAPConfiguration.getSecurityMode() + "\n" //
				+ "from a mobile phone or a computer.\n" //
				+ "Then, open http://" + host + ":" + port
				+ " in a browser to configure a Wi-Fi network with internet access.\n"
				+ "The demo will start automatically when a Wi-Fi network is configured successfully.\n" //
				+ "#############################################################\n");
	}

	@Override
	public void onSuccessfulJoin(AccessPointConfiguration apConfiguration) {
		LOGGER.info("Successfully joined Wi-Fi Network: " + apConfiguration.getSSID());
		// Update time on the board
		final String ntpServer = System.getProperty(NtpUtil.NTP_URL_PROPERTY, NtpUtil.NTP_DEFAULT_SERVER);
		final int port = Integer.getInteger(NtpUtil.NTP_PORT_PROPERTY, NtpUtil.NTP_DEFAULT_PORT).intValue();
		for (;;) {
			try {
				Thread.sleep(3_000);
				LOGGER.info("Updating local time from NTP server " + ntpServer + ":" + port);
				NtpUtil.updateLocalTime();
				break;
			} catch (IOException | InterruptedException e) {
				LOGGER.log(Level.SEVERE, "NTP ERROR," + e.getMessage() + ". Retrying...");
			}
		}

		// Start AWS DEMO
		try {
			new Aws().start();
		} catch (InterruptedException | AwsIotException | JSONException e) {
			LOGGER.log(Level.SEVERE, "AWS ERROR", e);
		}
	}

	@Override
	public void onTryingJoin(AccessPointConfiguration apConfiguration) {
		LOGGER.info("Trying Join:" + apConfiguration.getSSID());
	}

	@Override
	public void onSoftAPUnmount() {
		LOGGER.info("Soft AP Unmount");
	}

	@Override
	public void onSoftAPMountError(SoftAPConfiguration softAPConfiguration, IOException e) {
		LOGGER.info("Soft AP Mount Error: " + e.getMessage());
	}

	@Override
	public void onScan(AccessPoint[] accessPoints) {
		LOGGER.info("Scan:");
		for (int i = 0; i < accessPoints.length; i++) {
			LOGGER.info("-" + accessPoints[i].getSSID());
		}
	}

	@Override
	public void onJoinError(AccessPointConfiguration apConfiguration, Exception e) {
		LOGGER.log(Level.SEVERE, "Join Error: " + apConfiguration.getSSID(), e);
	}

}
