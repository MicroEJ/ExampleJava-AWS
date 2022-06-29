/*
 * Java
 *
 * Copyright 2018-2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot;

import java.util.logging.Logger;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import ej.aws.iot.AwsIotClient;
import ej.aws.iot.AwsIotException;
import ej.bon.TimerTask;

/**
 * Timer task that publishes data to a topic.
 */
@SuppressWarnings("nls")
public class PublishTimerTask extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(PublishTimerTask.class.getName());

	// Sample data to be published
	private static final String[] SAMPLE_DATA_PUBLISH_ARRAY = { "MicroEJ", "is", "a", "unique", "solution", "for",
			"building", "Internet", "of", "Things", "and", "embedded", "software", "and", "can", "now", "communicate",
			"with", "AWS IoT" };

	private int index = 0;

	// The AWS Client
	private final AwsIotClient awsClient;

	/**
	 * Initializes the timer task.
	 *
	 * @param awsClient
	 *            the AWS thing for being able to publish with it
	 */
	public PublishTimerTask(final AwsIotClient awsClient) {
		this.awsClient = awsClient;
	}

	@Override
	public void run() {
		try {
			JSONObject data = new JSONObject();
			data.put("message", SAMPLE_DATA_PUBLISH_ARRAY[this.index]);

			this.awsClient.publish(Aws.AWS_TOPIC_SAMPLE, data.toString().getBytes());

			this.index = (this.index + 1) % SAMPLE_DATA_PUBLISH_ARRAY.length;

		} catch (AwsIotException | JSONException e) {
			LOGGER.severe("An error occured while publishing. " + e.getMessage());
		}
	}

}
