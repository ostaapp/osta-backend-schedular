package com.dipcoin.partner.paymentGateway.model;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.scheduler.model.PlanDetail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dipcoin.partner.utils.PartnerClient.ChecksumFormat;

@Component
public class PartnerUtils {
	private static final Logger LOG = LogManager.getLogger(PartnerUtils.class);

	private static TypeReference<Map<String, List<PlanDetail>>> typeRef = new TypeReference<Map<String, List<PlanDetail>>>() {
	};

	private static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	ResourceLoader resourceLoader;

	private PartnerUtils() throws Exception {
	}

	public String computeChecksum(final byte[] rawPayload, ChecksumFormat format) {
		try {
			if (ChecksumFormat.CRC32.equals(format)) {
				CRC32 crc32 = new CRC32();
				crc32.update(rawPayload);
				return String.valueOf(crc32.getValue());
			}
			if (ChecksumFormat.SHA256.equals(format)) {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(rawPayload);
				return Hex.encodeHexString(md.digest());
			}
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(rawPayload);
			return Hex.encodeHexString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			LOG.error(LogFormatter.instance().message("Exception Caught").format(), e);
		}
		return null;
	}

	public HttpClientBuilder getSocketBuilder() {

		try {
			SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(new AtomicReference<KeyStore>().get(), new TrustSelfSignedStrategy()).build();

			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

			SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
					new String[] { "TLSv1.2" }, null, hostnameVerifier);

			RequestConfig config = RequestConfig.custom().setConnectTimeout(100000).setConnectionRequestTimeout(100000)
					.build();

			return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).disableAutomaticRetries()
					.setConnectionTimeToLive(100000, TimeUnit.MILLISECONDS).setDefaultRequestConfig(config);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			LOG.error("Failed to initialize partner payment gateway SSL socket builder.", e);
		}
		return null;
	}

	public Map<String, List<PlanDetail>> rechargeLookup() throws IOException {
		InputStream is = resourceLoader.getResource("classpath:recharge.json").getInputStream();
		Map<String, List<PlanDetail>> rechargeLookup = objectMapper.readValue(is, typeRef);
		if (is != null)
			is.close();

		return rechargeLookup;
	}
}
