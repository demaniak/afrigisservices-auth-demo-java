package com.afrigis.services.authdemo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * <p>
 * Demonstrates how to construct a valid URL for calling AfriGIS Services, as well
 * as calculating HMACs.
 * </p>
 *
 */
public class SampleApp {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * You can obtain a key and secret from <a href="https://developers.afrigis.co.za/sign-up/">AfriGIS</a>
     */
    private String key = "<YOUR KEY>";
    private String secret = "<YOUR SECRET>";

    /**
     * <p>
     * See https://developers.afrigis.co.za/ for more information
     * </p>
     */
    private String webservice = "intiendols.basic.geocode.address";
    private Boolean useTrial = false;
    private Boolean useTimestamp = true;
    private final String baseUrl = "https://saas.afrigis.co.za/rest/2/";

    private static final int ONE_SECOND = 1000;
    
    /**
     * To be replaced with stronger algorithm in the near future.
     */
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public static void main(String[] args) {
        String userInput = "Hatfield, Pretoria";
        SampleApp mainApp = new SampleApp();
        String fulLUrl = mainApp.buildUrl(userInput);
        System.out.println("Full URL:\n" + fulLUrl);
    }

    private String buildUrl(String searchTextFromUser) {
        String queryString = "ils_result_count=1&ils_location="
                + urlEncode(searchTextFromUser);
        
        String message = queryString + "/" + webservice + "/" + key;
        
        Long timestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                .getTimeInMillis() / ONE_SECOND;
        
        if (useTimestamp) {
            message = message + "/" + timestamp;
        }
        
        String authCode;
        
        if (useTrial) {
            authCode = "trial";
        } else {
            authCode = getHmac(key,secret.getBytes(UTF8),message);
        }
        
        String request = baseUrl + webservice + "/" + key + "/" + authCode;
        if (useTimestamp) {
            request = request + "/" + timestamp;
        }
        request = request + "/?" + queryString;
        
       return request;
    }
    
    /**
     * 
     * @param message
     *            the message tha needs to be signed
     * @return URL safe Base64 encoded HMAC value
     * @see Base64#encodeBase64URLSafeString(byte[])
     * @see SecretKeySpec
     * @see Mac
     */
    protected String getHmac(String saasClient, byte [] sharedKey,String message) {
        String hmac;
        try {

            //Generating HMAC over string 
            SecretKey signingKey = new SecretKeySpec(
                    sharedKey == null || sharedKey.length <= 0
                            ? saasClient.getBytes(UTF8) : sharedKey,
                    HMAC_SHA1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes("ASCII"));
            hmac = new String(Base64.encodeBase64(rawHmac), "UTF-8");
            
            hmac = fixHmac (hmac);

        } catch (Exception e) {
            e.printStackTrace();
            hmac = "ERROR";
        }

        return hmac;
    }

    private String urlEncode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return param;
        }
    }
    
    /**
     * Fixes up the HMAC String by replacing / and + with _ and - respectively.
     * Also removes pointless padding =
     * 
     * @param base64Hmac
     *            The HMAC String to fix up
     * @return Fixed HMAC
     */
    private String fixHmac(String base64Hmac) {
        String hmac2 = base64Hmac;

        hmac2 = hmac2.replace("/", "_")
        .replace("+", "-")
        .replace("=", "");

        return hmac2;
    }
}
