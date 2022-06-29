/*
 * Java
 *
 * Copyright 2018-2022 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */

package com.microej.demo.aws.iot;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ej.bon.Constants;

/**
 * This class provides functions to help creating key stores and trust stores for initializing the SSL context.
 */
@SuppressWarnings("nls")
public class SslContextBuilder {

	private static final String TLS_V_1_2 = "TLSv1.2";

	/**
	 * Builds an SSL context based BON Constants in test.constants.list file
	 *
	 * @return an configured SSL context.
	 */
	public static SSLContext getSslContext() {

		try {

			// Trust managers
			KeyStore tStore = KeyStore.getInstance(KeyStore.getDefaultType());
			tStore.load(null, null);
			tStore.setCertificateEntry("AmazonRootCA3",
					loadCertificate(Constants.getString("aws.trusted.server.certificate1")));
			tStore.setCertificateEntry("SFSRootCAG2",
					loadCertificate(Constants.getString("aws.trusted.server.certificate2")));
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
			trustManagerFactory.init(tStore);

			// key manager
			Certificate root = loadCertificate(Constants.getString("aws.root.certificate"));
			Certificate device = loadCertificate(Constants.getString("aws.device.certificate"));
			byte[] key = loadResource(Constants.getString("aws.device.key"));
			KeyStore kstore = KeyStore.getInstance(KeyStore.getDefaultType());
			kstore.load(null, null);
			kstore.setKeyEntry("device", key, new Certificate[] { device, root });
			KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			String password = Constants.getString("aws.device.keystore.password");
			km.init(kstore, password.toCharArray());

			// SSL context
			SSLContext context = SSLContext.getInstance(TLS_V_1_2);
			context.init(km.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			return context;
		} catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | UnrecoverableKeyException
				| KeyManagementException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param certPath
	 * @return
	 * @throws IOException
	 * @throws CertificateException
	 */
	private static Certificate loadCertificate(String certPath) throws IOException, CertificateException {
		Certificate cert;
		try (InputStream in = SslContextBuilder.class.getResourceAsStream(certPath)) {
			if (in == null) {
				throw new IllegalStateException("resource not found: " + certPath);
			}

			// Generate the server certificates
			cert = CertificateFactory.getInstance("X.509").generateCertificate(in);
		}
		return cert;
	}

	/**
	 * Loads the content of a resource into a byte array.
	 * <p>
	 * This method uses {@link Class#getResourceAsStream(String)} to load the resource.
	 *
	 * @param resourcePath
	 *            name of the resource
	 *
	 * @return an array of bytes filled-in with the content of the resource.
	 *
	 * @throws IOException
	 *             if an I/O error occurs during the stream reading.
	 *
	 * @see Class#getResourceAsStream(String)
	 */
	private static byte[] loadResource(String resourcePath) throws IOException {
		try (InputStream stream = SslContextBuilder.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IllegalStateException("resource not found " + resourcePath);
			}

			DataInputStream dataInputStream = new DataInputStream(stream);
			byte[] data = new byte[stream.available()];
			dataInputStream.readFully(data);
			return data;
		}
	}
}
