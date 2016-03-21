package com.gregdev.whirldroid.service;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.gregdev.whirldroid.WhirlpoolApiException;

/**
 * Downloads data from a URL
 * @author Greg
 */
public class HttpFetch {

	private int status_code;
	
	public int getStatusCode() {
		return status_code;
	}

	public String getDatafromURL(String url) throws WhirlpoolApiException {

		// initialise
		status_code = 0;

		// fetch data from URL
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet(url);
			getRequest.setHeader("User-Agent", "Whirldroid");
			HttpResponse getResponse = client.execute(getRequest);

			status_code = getResponse.getStatusLine().getStatusCode();
			if (status_code != 200) {
				switch (status_code) {
					case 401:
						throw new WhirlpoolApiException("Authentication failure - please check your API key.");
					case 403:
						throw new WhirlpoolApiException("An error has occurred. You may be in the penalty box or Whirlpool may be experiencing issues. Please load Whirlpool in your browser for more details.");
					case 500:
						throw new WhirlpoolApiException("Server error - please try again.");
					case 503:
						throw new WhirlpoolApiException("Whirlpool is down for maintenance.");
					case 509:
						throw new WhirlpoolApiException("Rate limit exceeded - please try again later.");
					default:
						throw new WhirlpoolApiException("An unknown error has occurred. Please try again.");
				}
			}

			HttpEntity getResponseEntity = getResponse.getEntity();
			if (getResponseEntity != null) {
				return EntityUtils.toString(getResponseEntity);
			}
		}
		catch (UnknownHostException e) {
			throw new WhirlpoolApiException("Unable to download data. Please try later.");
		}
		catch (ClientProtocolException e) {
			throw new WhirlpoolApiException("Unable to download data. Please try later.");
		}
		catch (IOException e) {
			throw new WhirlpoolApiException("Unable to download data. Please try later.");
		}
		
		return null;
	}
}
