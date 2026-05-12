package com.dipcoin.partner.utils;

import java.util.zip.CRC32;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChecksumUtil {

    private static final Logger LOG = LogManager.getLogger(ChecksumUtil.class);

    @Value("${bbps.api.checksum-key}")
    private String checksumKey;

    public String generateChecksum(String dataToCalculate) {
        try {
            String stringToHash = dataToCalculate + checksumKey;
            CRC32 crc = new CRC32();
            crc.update(stringToHash.getBytes());
            long checksum = crc.getValue();
            return Long.toString(checksum);
        } catch (Exception e) {
            LOG.error("Failed to generate BBPS checksum.", e);
            return null;
        }
    }
}
