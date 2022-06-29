/*
 * Java
 *
 * Copyright 2018-2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot;

import java.util.logging.Logger;

import ej.aws.iot.AwsIotMessage;
import ej.aws.iot.AwsIotMessageCallback;

/**
 * A topic listener that prints the received data.
 */
@SuppressWarnings("nls")
public class TopicSubscriber implements AwsIotMessageCallback {

	private static final Logger LOGGER = Logger.getLogger(TopicSubscriber.class.getName());

	@Override
	public void onMessageReceived(AwsIotMessage message) {
		// Here we have the topic on which the message is received and the data
		LOGGER.info("Message received on topic " + message.getTopic() + " => " + new String(message.getPayload()));
	}

}
