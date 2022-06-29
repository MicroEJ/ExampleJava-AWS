/*
 * Java
 *
 * Copyright 2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot;

import static ej.aws.iot.ShadowAction.delete;
import static ej.aws.iot.ShadowAction.get;
import static ej.aws.iot.ShadowAction.update;
import static ej.aws.iot.ShadowResult.accepted;
import static ej.aws.iot.ShadowResult.delta;
import static ej.aws.iot.ShadowResult.documents;
import static ej.aws.iot.ShadowResult.rejected;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import com.microej.demo.aws.iot.shadow.DeleteAccepted;
import com.microej.demo.aws.iot.shadow.DeleteRejected;
import com.microej.demo.aws.iot.shadow.GetAccepeted;
import com.microej.demo.aws.iot.shadow.GetRejected;
import com.microej.demo.aws.iot.shadow.UpdateAccepted;
import com.microej.demo.aws.iot.shadow.UpdateDelta;
import com.microej.demo.aws.iot.shadow.UpdateRejected;

import ej.aws.iot.AwsIotClient;
import ej.aws.iot.AwsIotClientOptions;
import ej.aws.iot.AwsIotClientOptions.Builder;
import ej.aws.iot.AwsIotException;
import ej.bon.Constants;
import ej.bon.Timer;
import ej.bon.Util;

/**
 * AWS client configuration and test flow
 *
 * This class do the following:
 *
 * - Configure an AWS client instance
 *
 * - Perform a Just in time provisioning of the device if necessary
 *
 * - Subscribe to the test 'AWS_TOPIC_SAMPLE' topic
 *
 * - Start publishing a message to 'AWS_TOPIC_SAMPLE' topic every 2 seconds
 *
 * - Subscribe to default Device shadow updates
 *
 * - Perform some device shadow modifications
 */
@SuppressWarnings("nls")
public class Aws {

	private static final Logger LOGGER = Logger.getLogger(Aws.class.getName());

	/**
	 * Example pub/sub topic.
	 */
	public static final String AWS_TOPIC_SAMPLE = "awsiot/demo/sample"; //$NON-NLS-1$

	private static final int SLEEP_TIME = 1000;

	private static final int AWS_MAX_CONNECTION_RETRIES = 3;

	/**
	 * AWS IoT client
	 */
	private final AwsIotClient awsClient;

	/**
	 * Constructor
	 */
	public Aws() {
		// AWS IoT Client options
		final AwsIotClientOptions options = Builder.builder() //
				.host(Constants.getString("aws.url"))//
				.port(Constants.getInt("aws.port"))//
				.thingName(Constants.getString("aws.thing.name"))//
				.clientID(Constants.getString("aws.thing.name"))//
				.secure(SslContextBuilder.getSslContext().getSocketFactory())//
				.timeout(60)//
				.keepAlive(60)//
				.build();

		this.awsClient = new AwsIotClient(options);
	}

	/**
	 * Start test flow.
	 *
	 * @throws InterruptedException
	 *             on error while retrying the connection during the provisioning
	 * @throws AwsIotException
	 *             on error with AWS client
	 * @throws JSONException
	 *             on error with json data used to update the Shadow
	 */

	public void start() throws InterruptedException, AwsIotException, JSONException {

		// Connect my AWS IoT Thing (my device) to the broker
		LOGGER.info("Connecting to AWS IoT Core Server. JIT provisioning will be done if necessary.");
		justInTimeProvisionAndConnect();
		LOGGER.info("Device connected to the broker."); //$NON-NLS-1$

		// Add a listener on the sample topic
		this.awsClient.subscribe(AWS_TOPIC_SAMPLE, new TopicSubscriber());
		LOGGER.info("Update listener added, we're now subscribed to the topic " + AWS_TOPIC_SAMPLE); //$NON-NLS-1$

		// Schedule a timer task that publishes sample messages to a topic every 2 seconds
		Timer samplePublishTimer = new Timer();
		samplePublishTimer.schedule(new PublishTimerTask(this.awsClient), 0, 2000);
		LOGGER.info("Sample data publishing timer task initialized."); //$NON-NLS-1$

		// AWS IoT / Shadow Management
		// Subscribe to shadow result on every action (get, delete, update)
		this.awsClient.subscribeToShadow(get, accepted, new GetAccepeted());
		this.awsClient.subscribeToShadow(get, rejected, new GetRejected());

		this.awsClient.subscribeToShadow(delete, accepted, new DeleteAccepted());
		this.awsClient.subscribeToShadow(delete, rejected, new DeleteRejected());

		this.awsClient.subscribeToShadow(update, accepted, new UpdateAccepted());
		this.awsClient.subscribeToShadow(update, rejected, new UpdateRejected());
		this.awsClient.subscribeToShadow(update, delta, new UpdateDelta());
		this.awsClient.subscribeToShadow(update, documents, new UpdateDelta());

		// Report device state
		LOGGER.info("Create or Update Device Shadow by reporting the device state"); //$NON-NLS-1$

		// device location
		JSONObject location = new JSONObject();
		location.put("country", "FR");
		location.put("city", "Nantes");

		// device capabilities
		JSONObject capabilities = new JSONObject();
		capabilities.put("network", "WIFI");
		capabilities.put("ble", true);
		capabilities.put("ota", true);

		JSONObject device = new JSONObject();
		device.put("location", location);
		device.put("firmware-version", "1.8.3");
		device.put("capabilities", capabilities);
		device.put("state", "ready"); // report firmware update state here for example
		device.put("timestamp", Util.currentTimeMillis());

		// Shadow format
		JSONObject reported = new JSONObject();
		reported.put("reported", device);
		JSONObject state = new JSONObject();
		state.put("state", reported);

		this.awsClient.updateShadow(state.toString().getBytes());
	}

	/**
	 * Just in time provision and connect client to AWS IoT platform.
	 *
	 * @param client
	 * @throws InterruptedException
	 */
	private void justInTimeProvisionAndConnect() throws InterruptedException {

		try {
			// If this is the first time the thing is trying to connect to AWS,
			// it will fail with connection lost and start the JITP process
			// we retry the connection when this happens.
			// The client should connect successfully when it provisioned by AWS
			this.awsClient.connect();

		} catch (final AwsIotException e) {
			retryConnection();
		}
	}

	/**
	 * Retry the connection. This is used in JITP process
	 *
	 * @throws InterruptedException
	 */
	private void retryConnection() throws InterruptedException {
		int retryCount = 1;
		while (retryCount <= AWS_MAX_CONNECTION_RETRIES) {
			try {

				Thread.sleep(retryCount * SLEEP_TIME); // exponential wait
				LOGGER.info("Connection lost: retrying the connection (" + retryCount + ") ..."); //$NON-NLS-1$ //$NON-NLS-2$
				this.awsClient.connect();
				return; // client connected, stop retrying and break while loop.

			} catch (final AwsIotException e) {
				retryCount++;
				if (e.getCause() == null || !(e.getCause() instanceof MqttException)) {
					// if the cause is not an MQTT error throw an exception and do not retry
					throw new IllegalStateException(e);
				}

				final MqttException error = (MqttException) e.getCause();
				if (error.getReasonCode() == MqttException.REASON_CODE_CLIENT_CONNECTED) {
					// client is already connected.
					// this can happen when connection state change really fast during an auto provisioning
					return; // break the while loop
				} else if (error.getReasonCode() != MqttException.REASON_CODE_CONNECTION_LOST) {
					// throw an exception if the error is not a connection lost
					throw new IllegalStateException(e);
				}
			}
		}
	}
}
