package com.xinchang.common.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HttpSender {
	
	 private static final Logger  logger = LoggerFactory.getLogger(HttpSender.class);
	/**
	 * 
	 * @param url
	 *            应用地址，类似于http://ip:port/sms/send
	 * @param map
	 *            参数列表
	 * @return 返回值定义参见协议文档
	 * @throws Exception
	 */
	public static String send(String url, Map<String, String> map) {
		
		logger.info("url:{},map:{}",url,map);
		
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpPost httppost = new HttpPost(url);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		Set<String> set = map.keySet();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String name = it.next();
			params.add(new BasicNameValuePair(name, map.get(name)));
		}

		try {
			httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			return EntityUtils.toString(entity, StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

}
