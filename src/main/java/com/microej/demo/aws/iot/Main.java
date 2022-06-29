/*
 * Java
 *
 * Copyright 2018-2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot;

import java.io.IOException;
import java.util.logging.Logger;

import com.microej.example.wifi.setup.ConfigurationManager;
import com.microej.example.wifi.setup.web.WebSoftAPConnector;

import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.SoftAPConfiguration;
import ej.net.util.wifi.AccessPointConfiguration;

/**
 * Main class of the demonstration use the launchers, either [SIM] or [EMB] to run it. <br>
 *
 * 1) Connect to WIFI Network. See {@link Wifi}
 *
 * 2) Perform a Just in time provisioning of the device if necessary. See {@link Aws}
 *
 * 3) Subscribe to the test 'AWS_TOPIC_SAMPLE' topic see {@link TopicSubscriber}
 *
 * 4) Start publishing a message to 'AWS_TOPIC_SAMPLE' topic every 2 seconds see {@link PublishTimerTask}
 *
 * 5) Subscribe to default Device shadow updates. See {@link Aws}
 *
 * 6) Perform some device shadow modifications. See {@link Aws}
 *
 *
 */
@SuppressWarnings("nls")
public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	/**
	 * Ensure port 80 is available on your device or change this to a free one.
	 */
	private static final int WIFI_WEBAPP_CONFIG_PORT = 80;
	private static final String SOFT_AP_SSID = "AWS_IOT_SAMPLE"; //$NON-NLS-1$
	private static final String SOFT_AP_PASSPHRASE = "qwertyuiop"; //$NON-NLS-1$
	private static final SecurityMode SOFT_AP_SECURITY = SecurityMode.WPA2;

	/**
	 * Soft Access Point Connector (WIFI)
	 */
	private static WebSoftAPConnector webSoftAPConnector;

	/**
	 * Entry point of the application.
	 *
	 * @param args
	 *            the program arguments
	 * @throws IOException
	 *             on WIFI configuration errors
	 *
	 */
	public static void main(String[] args) throws IOException {
		webSoftAPConnector = new WebSoftAPConnector(new ConfigurationManager() {

			private AccessPointConfiguration config = null;

			@Override
			public void storeAPConfiguration(final AccessPointConfiguration config) {
				LOGGER.info("Storing AP config for reuse");
				// can be stored to the FS for later reuse.
				this.config = config;
			}

			@Override
			public AccessPointConfiguration loadAPConfiguration() {
				LOGGER.info("Loading Stored AP config");
				return this.config;
			}

			@Override
			public SoftAPConfiguration getSoftAPConfiguration() {
				final SoftAPConfiguration config = new SoftAPConfiguration();
				config.setName(SOFT_AP_SSID);
				config.setSSID(SOFT_AP_SSID);
				config.setPassphrase(SOFT_AP_PASSPHRASE);
				config.setSecurityMode(SOFT_AP_SECURITY);
				return config;
			}
		}, WIFI_WEBAPP_CONFIG_PORT);
		webSoftAPConnector.addListener(new Wifi(webSoftAPConnector));
		webSoftAPConnector.start();
	}

}
