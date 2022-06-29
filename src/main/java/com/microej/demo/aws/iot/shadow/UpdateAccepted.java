/*
 * Java
 *
 * Copyright 2021-2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */
package com.microej.demo.aws.iot.shadow;

import java.util.logging.Logger;

import ej.aws.iot.AwsIotMessage;
import ej.aws.iot.AwsIotMessageCallback;

/**
 * Executed when a shadow is updated successfully. The received message contains the update result.
 *
 */
@SuppressWarnings("nls")
public class UpdateAccepted implements AwsIotMessageCallback {

	private static final Logger LOGGER = Logger.getLogger(UpdateAccepted.class.getName());

	@Override
	public void onMessageReceived(AwsIotMessage message) {
		LOGGER.fine("Message received on topic='" + message.getTopic() + ", payload='"
				+ new String(message.getPayload()) + "'");

	}

}
