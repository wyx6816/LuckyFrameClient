package luckyclient.utils.httputils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import luckyclient.remote.entity.ProjectProtocolTemplate;
import luckyclient.utils.Constants;
import luckyclient.utils.LogUtil;

/**
 * =================================================================
 * ����һ�������Ƶ��������������������κ�δ��������ǰ���¶Գ����������޸ĺ�������ҵ��;��Ҳ�������Գ�������޸ĺ����κ���ʽ�κ�Ŀ�ĵ��ٷ�����
 * Ϊ���������ߵ��Ͷ��ɹ���LuckyFrame�ؼ���Ȩ��Ϣ�Ͻ��۸�
 * ���κ����ʻ�ӭ��ϵ�������ۡ� QQ:1573584944  seagull1985
 * =================================================================
 * 
 * @author�� seagull
 * @date 2017��12��1�� ����9:29:40
 * 
 */
public class HttpClientTools {
	/**
	 * ʹ��HttpURLConnection����post����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param timeout
	 * @param headmsg
	 * @return
	 */
	public static String sendHttpURLPost(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		// �����������
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendHttpURLPost����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı����޷�ʹ��sendHttpURLPost����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> e : params.entrySet()) {
					sbParams.append(e.getKey());
					sbParams.append("=");
					sbParams.append(e.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����HTTPURLPost������Ϣ...key:��{}��    value:��{}��",e.getKey(),e.getValue());
				}
			}
		}
		HttpURLConnection con = null;
		OutputStreamWriter osw = null;
		BufferedReader br = null;
		// ��������
		try {
			URL url = new URL(urlParam);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setConnectTimeout(timeout*1000);
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻HTTPURLPostͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key, value);
		    	}else{
		    		con.setRequestProperty(key, value);
		    	}
	        }
			if (sbParams != null && sbParams.length() > 0) {
				osw = new OutputStreamWriter(con.getOutputStream(), charset);
				osw.write(sbParams.substring(0, sbParams.length() - 1));
				osw.flush();
			}
			// ��ȡ��������
			resultBuffer = new StringBuffer();
			int contentLength =0;
			if(null!=con.getHeaderField("Content-Length")){
				contentLength = Integer.parseInt(con.getHeaderField("Content-Length"));
			}
			
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getResponseCode()+Constants.RESPONSE_END);
			}
			if (contentLength > 0||"chunked".equals(con.getHeaderField("Transfer-Encoding"))) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
				String temp;
				while ((temp = br.readLine()) != null) {
					resultBuffer.append(temp);
				}
			}else{
				resultBuffer.append("Content-Length=0");
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpURLConnection����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpURLConnection����post�����ر��������쳣�����飡", e);
					osw = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpURLConnection����post�����ر��������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
		}

		return resultBuffer.toString();
	}

	/**
	 * ʹ��URLConnection����post
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param timeout
	 * @param headmsg
	 * @return
	 */
	public static String sendURLPost(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {		
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		// �����������
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendURLPost����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı����޷�ʹ��sendURLPost����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> e : params.entrySet()) {
					sbParams.append(e.getKey());
					sbParams.append("=");
					sbParams.append(e.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����URLPost������Ϣ...key:��{}��    value:��{}��",e.getKey(),e.getValue());
				}
			}
		}
		URLConnection con = null;
		OutputStreamWriter osw = null;
		BufferedReader br = null;
		try {
			URL realUrl = new URL(urlParam);
			// �򿪺�URL֮�������
			con = realUrl.openConnection();
			// ����ͨ�õ���������
			con.setRequestProperty("accept", "*/*");
			con.setRequestProperty("connection", "Keep-Alive");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻URLPostͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key, value);
		    	}else{
		    		con.setRequestProperty(key, value);
		    	}
	        }
		    
			con.setConnectTimeout(timeout*1000);
			// ����POST�������������������
			con.setDoOutput(true);
			con.setDoInput(true);
			// ��ȡURLConnection�����Ӧ�������
			osw = new OutputStreamWriter(con.getOutputStream(), charset);
			if (sbParams != null && sbParams.length() > 0) {
				// �����������
				osw.write(sbParams.substring(0, sbParams.length() - 1));
				// flush������Ļ���
				osw.flush();
			}
			// ����BufferedReader����������ȡURL����Ӧ
			resultBuffer = new StringBuffer();
			int contentLength =0;
			if(null!=con.getHeaderField("Content-Length")){
				contentLength = Integer.parseInt(con.getHeaderField("Content-Length"));
			}
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getHeaderField(null)+Constants.RESPONSE_END);
			}
			if (contentLength >= 0||"chunked".equals(con.getHeaderField("Transfer-Encoding"))) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
				String temp;
				while ((temp = br.readLine()) != null) {
					resultBuffer.append(temp);
				}
			}else{
					resultBuffer.append("Content-Length=0");
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��URLConnection����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��URLConnection����post�����ر�osw�������쳣�����飡", e);
					osw = null;
					throw new RuntimeException(e);
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��URLConnection����post�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}
		return resultBuffer.toString();
	}

	/**
	 * ����get���󱣴������ļ�
	 * @param urlParam
	 * @param params
	 * @param fileSavePath
	 * @param timeout
	 * @param headmsg
	 */
	public static String sendGetAndSaveFile(String urlParam, Map<String, Object> params, String fileSavePath, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {
		// �����������
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendGetAndSaveFile����(����Ϊkey-value)...");
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					sbParams.append(entry.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����HTTPSaveFile������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}
		}
		HttpURLConnection con = null;
		BufferedReader br = null;
		FileOutputStream os = null;
		try {
			URL url = null;
			if (sbParams != null && sbParams.length() > 0) {
				url = new URL(urlParam + "?" + sbParams.substring(0, sbParams.length() - 1));
			} else {
				url = new URL(urlParam);
			}
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻HTTPSaveFileͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key, value);
		    	}else{
		    		con.setRequestProperty(key, value);
		    	}
	        }
			con.setConnectTimeout(timeout*1000);
			con.connect();
			// ����BufferedReader����������ȡURL����Ӧ
			StringBuffer resultBuffer = new StringBuffer();
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getResponseCode()+Constants.RESPONSE_END);
			}
			InputStream is = con.getInputStream();
			os = new FileOutputStream(fileSavePath);
			byte buf[] = new byte[1024];
			int count = 0;
			while ((count = is.read(buf)) != -1) {
				os.write(buf, 0, count);
			}
			os.flush();
			return resultBuffer.toString()+"�����ļ��ɹ�����ǰ���ͻ���·��:" + fileSavePath + " �鿴������";
		} catch (Exception e) {
			LogUtil.APP.error("����get���󱣴������ļ������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					LogUtil.APP.error("����get���󱣴������ļ���ر�OS�������쳣�����飡", e);
					os = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("����get���󱣴������ļ���ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
		}
	}

	/**
	 * ʹ��HttpURLConnection����get����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param timeout
	 * @param headmsg
	 * @return
	 */
	public static String sendHttpURLGet(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		// �����������
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendHttpURLGet����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendHttpURLGet����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					sbParams.append(entry.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����HTTPURLGet������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}
		}
		HttpURLConnection con = null;
		BufferedReader br = null;
		try {
			URL url = null;
			if (sbParams != null && sbParams.length() > 0) {
				url = new URL(urlParam + "?" + sbParams.substring(0, sbParams.length() - 1));
			} else {
				url = new URL(urlParam);
			}
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻HTTPURLGetͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key, value);
		    	}else{
		    		con.setRequestProperty(key, value);
		    	}
	        }
			con.setConnectTimeout(timeout*1000);
			con.connect();
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getResponseCode()+Constants.RESPONSE_END);
			}
			br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
			String temp;
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}
			if(resultBuffer.length()==0){
				resultBuffer.append("��ȡ��������Ӧ�����쳣!��Ӧ�룺"+con.getResponseCode());
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpURLConnection����get��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpURLConnection����get�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
		}
		return resultBuffer.toString();
	}

	/**
	 * ʹ��URLConnection����get����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param timeout
	 * @param headmsg
	 * @return
	 */
	public static String sendURLGet(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		// �����������
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendURLGet����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendURLGet����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					sbParams.append(entry.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����URLGet������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}

		}
		BufferedReader br = null;
		try {
			URL url = null;
			if (sbParams != null && sbParams.length() > 0) {
				url = new URL(urlParam + "?" + sbParams.substring(0, sbParams.length() - 1));
			} else {
				url = new URL(urlParam);
			}
			URLConnection con = url.openConnection();
			// ������������
			con.setRequestProperty("accept", "*/*");
			con.setRequestProperty("connection", "Keep-Alive");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻URLGetͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key, value);
		    	}else{
		    		con.setRequestProperty(key, value);
		    	}
	        }
			con.setConnectTimeout(timeout*1000);
			// ��������
			con.connect();
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getHeaderField(null)+Constants.RESPONSE_END);
			}
			br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
			String temp;
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}
			if(resultBuffer.length()==0){
				resultBuffer.append("��ȡ��������Ӧ�����쳣!");
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��URLConnection����get��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��URLConnection����get�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}
		return resultBuffer.toString();
	}

	/**
	 * ʹ��HttpClient��JSON��ʽ����post����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String httpClientPostJson(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws NoSuchAlgorithmException, KeyManagementException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		HttpPost httpPost = new HttpPost(urlParam);
	    httpPost.setHeader("Content-Type", "application/json");
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout)  
	             //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build();  
	    httpPost.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻HTTPPostJsonͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpPost.setHeader(key, value);
	    	}else{
	    		httpPost.setHeader(key, value);
	    	}
        }
		// �����������
		BufferedReader br = null;
		try {
		if(params.size()>0){
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.info("�������ͣ�TEXT,����HTTPPostJson������Ϣ...��{}��",params.get("_forTextJson").toString());
				StringEntity entity = new StringEntity(params.get("_forTextJson").toString(),charset);
				httpPost.setEntity(entity);
			}else{
			    String jsonString = JSON.toJSONString(params);
				LogUtil.APP.info("�������ͣ�FORM,����HTTPPostJson������Ϣ...��{}��",jsonString);
				StringEntity entity = new StringEntity(jsonString,charset);
				httpPost.setEntity(entity);
			}
		}
       
		 CloseableHttpResponse response = httpclient.execute(httpPost);

		// ��ȡ��������Ӧ����
		resultBuffer = new StringBuffer();
		if(1==responsehead){
			Header[] headmsgstr=response.getAllHeaders();
			resultBuffer.append("RESPONSE_HEAD:��{");
			for(Header header:headmsgstr){
				resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
			}
			resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
		}
		if(1==responsecode){
			resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
		}
		if(null!=response.getEntity()){
			br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
			String temp;
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}	
		}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient��JSON��ʽ����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient��JSON��ʽ����post�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}		
		return resultBuffer.toString();
	}
	
	/**
	 * ʹ��HttpClient����post����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String httpClientPost(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws NoSuchAlgorithmException, KeyManagementException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		HttpPost httpPost = new HttpPost(urlParam);
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout) 
	            //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build(); 
	    httpPost.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻HTTPClientPostͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpPost.setHeader(key, value);
	    	}else{
	    		httpPost.setHeader(key, value);
	    	}
        }
		// �����������
		BufferedReader br = null;
		try {
			if(params.size()>0){
				if(1==params.size()&&params.containsKey("_forTextJson")){
					LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientPost����(����Ϊkey-value)...");
					return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientPost����(����Ϊkey-value)...";
				}else{
					//ƴ�Ӳ���
				    List <NameValuePair> nvps = new ArrayList <NameValuePair>();
				    for (Map.Entry<String, Object> m :params.entrySet())  { 
			            nvps.add(new BasicNameValuePair(m.getKey(), m.getValue().toString()));
			            LogUtil.APP.info("����HTTPClientPost������Ϣ...key:��{}��    value:��{}��",m.getKey(),m.getValue());
			        }
				    httpPost.setEntity(new UrlEncodedFormEntity(nvps,charset));
				}
			}

			 CloseableHttpResponse response = httpclient.execute(httpPost);
			// ��ȡ��������Ӧ����
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Header[] headmsgstr=response.getAllHeaders();
				resultBuffer.append("RESPONSE_HEAD:��{");
				for(Header header:headmsgstr){
					resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
				}
				resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
			}
			br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
			String temp;
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}
			if(resultBuffer.length()==0){
				resultBuffer.append("��ȡ��������Ӧ�����쳣����Ӧ�룺"+response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient����post�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}
		
		return resultBuffer.toString();
	}

	/**
	 * ʹ��HttpClient�ϴ��ļ�
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String httpClientUploadFile(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws NoSuchAlgorithmException, KeyManagementException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		HttpPost httpPost = new HttpPost(urlParam);
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout)  
	             //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build();  
	    httpPost.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻httpClientUploadFileͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpPost.setHeader(key, value);
	    	}else{
	    		httpPost.setHeader(key, value);
	    	}
        }
		// �����������
		BufferedReader br = null;
		try {
			if(params.size()>0){
				if(1==params.size()&&params.containsKey("_forTextJson")){
					LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientUploadFile����(����Ϊkey-value)...");
					return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientUploadFile����(����Ϊkey-value)...";
				}else{
					//ƴ�Ӳ���
					MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
					//��������ı����ʽ  
					entityBuilder.setCharset(Charset.forName(charset));
					
				    for (Map.Entry<String, Object> m :params.entrySet())  {
				    	if (m.getValue() instanceof File) {
				    		entityBuilder.addBinaryBody(m.getKey(), (File)m.getValue());
				    		LogUtil.APP.info("����httpClientUploadFile �ϴ��ļ�������Ϣ...key:��{}��    value:��{}��",m.getKey(),m.getValue());
				    	}else{
				    		entityBuilder.addTextBody(m.getKey(), m.getValue().toString());
				    		LogUtil.APP.info("����httpClientUploadFile������Ϣ...key:��{}��    value:��{}��",m.getKey(),m.getValue());
				    	}
			        }
				    HttpEntity reqEntity =entityBuilder.build();
				    httpPost.setEntity(reqEntity);
				}
			}

			 CloseableHttpResponse response = httpclient.execute(httpPost);
			// ��ȡ��������Ӧ����
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Header[] headmsgstr=response.getAllHeaders();
				resultBuffer.append("RESPONSE_HEAD:��{");
				for(Header header:headmsgstr){
					resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
				}
				resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
			}
			br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
			String temp;
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}
			if(resultBuffer.length()==0){
				resultBuffer.append("��ȡ��������Ӧ�����쳣����Ӧ�룺"+response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient�ϴ��ļ������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient�ϴ��ļ���ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}
		return resultBuffer.toString();
	}
	
	/**
	 * ʹ��HttpClient����get����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String httpClientGet(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws NoSuchAlgorithmException, KeyManagementException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);	
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		BufferedReader br = null;
		// �����������
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientGet����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientGet����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					try {
						sbParams.append(URLEncoder.encode(String.valueOf(entry.getValue()), charset));
					} catch (UnsupportedEncodingException e) {
						LogUtil.APP.error("ʹ��HttpClient����get����ƴ��URLʱ�����쳣�����飡", e);
						throw new RuntimeException(e);
					}
					sbParams.append("&");
					LogUtil.APP.info("����HTTPClientGet������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}
			
		}
		if (sbParams != null && sbParams.length() > 0) {
			urlParam = urlParam + "?" + sbParams.substring(0, sbParams.length() - 1);
		}
		HttpGet httpGet = new HttpGet(urlParam);
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout) 
	            //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build(); 
	    httpGet.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻HTTPClientGetͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpGet.setHeader(key, value);
	    	}else{
	    		httpGet.setHeader(key, value);
	    	}
        }
		try {
			CloseableHttpResponse response = httpclient.execute(httpGet);
			
			// ��ȡ��������Ӧ����
			br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
			String temp;
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Header[] headmsgstr=response.getAllHeaders();
				resultBuffer.append("RESPONSE_HEAD:��{");
				for(Header header:headmsgstr){
					resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
				}
				resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
			}
			while ((temp = br.readLine()) != null) {
				resultBuffer.append(temp);
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient����get��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient����get�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}
		return resultBuffer.toString();
	}

	/**
	 * ʹ��socket����post����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @return
	 */
	public static String sendSocketPost(String urlParam, Map<String, Object> params, String charset,
			Map<String, String> headmsg) {
		String result = "";
		LogUtil.APP.info("����Socket�����ַ:��{}��",urlParam);
		// �����������
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendSocketPost����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendSocketPost����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					sbParams.append(entry.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����SocketPost������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}
		}
		Socket socket = null;
		OutputStreamWriter osw = null;
		InputStream is = null;
		try {
			URL url = new URL(urlParam);
			String host = url.getHost();
			int port = url.getPort();
			if (-1 == port) {
				port = 80;
			}
			String path = url.getPath();
			socket = new Socket(host, port);
			StringBuffer sb = new StringBuffer();
			sb.append("POST " + path + " HTTP/1.1\r\n");
			sb.append("Host: " + host + "\r\n");
			sb.append("Connection: Keep-Alive\r\n");
			sb.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻Socketͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		sb.append(key+": "+value+" \r\n");
		    	}else{
		    		sb.append(key+": "+value+" \r\n");
		    	}
	        }
			sb.append("Content-Length: ").append(sb.toString().getBytes().length).append("\r\n");
			// ����һ���س����У���ʾ��Ϣͷд�꣬��Ȼ������������ȴ�
			sb.append("\r\n");
			if (sbParams != null && sbParams.length() > 0) {
				sb.append(sbParams.substring(0, sbParams.length() - 1));
			}
			osw = new OutputStreamWriter(socket.getOutputStream());
			osw.write(sb.toString());
			osw.flush();
			is = socket.getInputStream();
			String line = null;
			// ��������Ӧ�����ݳ���
			int contentLength = 0;
			// ��ȡhttp��Ӧͷ����Ϣ
			do {
				line = readLine(is, 0, charset);
				if (line.startsWith("Content-Length")) {
					// �õ���Ӧ�����ݳ���
					contentLength = Integer.parseInt(line.split(":")[1].trim());
				}
				// ���������һ�������Ļس����У����ʾ����ͷ����
			} while (!"\r\n".equals(line));
			// ��ȡ����Ӧ�����ݣ�������Ҫ�����ݣ�
			result = readLine(is, contentLength, charset);
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��socket����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��socket����post�����ر�osw�������쳣�����飡", e);
					osw = null;
					throw new RuntimeException(e);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							LogUtil.APP.error("ʹ��socket����post�����ر�socket�����쳣�����飡", e);
							socket = null;
							throw new RuntimeException(e);
						}
					}
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��socket����post�����ر�socket is��������쳣�����飡", e);
					is = null;
					throw new RuntimeException(e);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							socket = null;
							LogUtil.APP.error("ʹ��socket����post�����ر�socket�����쳣�����飡", e);
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * ʹ��socket����get����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @return
	 */
	public static String sendSocketGet(String urlParam, Map<String, Object> params, String charset,Map<String, String> headmsg) {
		String result = "";
		LogUtil.APP.info("����Socket�����ַ:��{}��",urlParam);
		// �����������
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendSocketGet����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendSocketGet����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> entry : params.entrySet()) {
					sbParams.append(entry.getKey());
					sbParams.append("=");
					sbParams.append(entry.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����SocketPost������Ϣ...key:��{}��    value:��{}��",entry.getKey(),entry.getValue());
				}
			}

		}
		Socket socket = null;
		OutputStreamWriter osw = null;
		InputStream is = null;
		try {
			URL url = new URL(urlParam);
			String host = url.getHost();
			int port = url.getPort();
			if (-1 == port) {
				port = 80;
			}
			String path = url.getPath();
			socket = new Socket(host, port);
			StringBuffer sb = new StringBuffer();
			sb.append("GET " + path + " HTTP/1.1\r\n");
			sb.append("Host: " + host + "\r\n");
			sb.append("Connection: Keep-Alive\r\n");
			sb.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻Socketͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		sb.append(key+": "+value+" \r\n");
		    	}else{
		    		sb.append(key+": "+value+" \r\n");
		    	}
	        }
			sb.append("Content-Length: ").append(sb.toString().getBytes().length).append("\r\n");
			// ����һ���س����У���ʾ��Ϣͷд�꣬��Ȼ������������ȴ�
			sb.append("\r\n");
			if (sbParams != null && sbParams.length() > 0) {
				sb.append(sbParams.substring(0, sbParams.length() - 1));
			}
			osw = new OutputStreamWriter(socket.getOutputStream());
			osw.write(sb.toString());
			osw.flush();
			is = socket.getInputStream();
			String line = null;
			// ��������Ӧ�����ݳ���
			int contentLength = 0;
			// ��ȡhttp��Ӧͷ����Ϣ
			do {
				line = readLine(is, 0, charset);
				if (line.startsWith("Content-Length")) {
					// �õ���Ӧ�����ݳ���
					contentLength = Integer.parseInt(line.split(":")[1].trim());
				}
				// ���������һ�������Ļس����У����ʾ����ͷ����
			} while (!"\r\n".equals(line));
			// ��ȡ����Ӧ�����ݣ�������Ҫ�����ݣ�
			result = readLine(is, contentLength, charset);
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��socket����get��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��socket����get�����ر�osw�������쳣�����飡", e);
					osw = null;
					throw new RuntimeException(e);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							LogUtil.APP.error("ʹ��socket����get�����ر�socket�����쳣�����飡", e);
							socket = null;
							throw new RuntimeException(e);
						}
					}
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��socket����get�����ر�socket is��������쳣�����飡", e);
					is = null;
					throw new RuntimeException(e);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							LogUtil.APP.error("ʹ��socket����get�����ر�socket��������쳣�����飡", e);
							socket = null;
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * ��ȡһ�����ݣ�contentLe���ݳ���Ϊ0ʱ����ȡ��Ӧͷ��Ϣ����Ϊ0ʱ������
	 * @param is
	 * @param contentLength
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	private static String readLine(InputStream is, int contentLength, String charset) throws IOException {
		List<Byte> lineByte = new ArrayList<Byte>();
		byte tempByte;
		int cumsum = 0;
		if (contentLength != 0) {
			do {
				tempByte = (byte) is.read();
				lineByte.add(Byte.valueOf(tempByte));
				cumsum++;
				// cumsum����contentLength��ʾ�Ѷ���
			} while (cumsum < contentLength);
		} else {
			do {
				tempByte = (byte) is.read();
				lineByte.add(Byte.valueOf(tempByte));
				// ���з���ascii��ֵΪ10
			} while (tempByte != 10);
		}

		byte[] resutlBytes = new byte[lineByte.size()];
		for (int i = 0; i < lineByte.size(); i++) {
			resutlBytes[i] = (lineByte.get(i)).byteValue();
		}
		return new String(resutlBytes, charset);
	}

	/**
	 * ʹ��HttpURLConnection����delete����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param timeout
	 * @param headmsg
	 * @return
	 */
	public static String sendHttpURLDel(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) {
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout();
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		// �����������
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		StringBuffer sbParams = new StringBuffer();
		if (params != null && params.size() > 0) {
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendHttpURLDel����(����Ϊkey-value)...");
				return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��sendHttpURLDel����(����Ϊkey-value)...";
			}else{
				for (Entry<String, Object> e : params.entrySet()) {
					sbParams.append(e.getKey());
					sbParams.append("=");
					sbParams.append(e.getValue());
					sbParams.append("&");
					LogUtil.APP.info("����HttpURLDel������Ϣ...key:��{}��    value:��{}��",e.getKey(),e.getValue());
				}
			}
		}
		HttpURLConnection con = null;
		OutputStreamWriter osw = null;
		BufferedReader br = null;
		// ��������
		try {
			URL url = new URL(urlParam);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("DELETE");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/json");
			//�滻ͷ����Ϣ
		    for (Map.Entry<String, String> m :headmsg.entrySet())  {
		    	String key=m.getKey();
		    	String value=m.getValue();
		    	LogUtil.APP.info("��ʼ����|�滻HTTPͷ����Ϣ...key:��{}��    value:��{}��",key,value);
		    	if(null!=value&&value.indexOf("Base64(")==0){
		    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
		    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
		    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
		    		con.setRequestProperty(key,value);
		    	}else{
		    		con.setRequestProperty(key,value);
		    	}
	        }
			con.setConnectTimeout(timeout*1000);
			if (sbParams != null && sbParams.length() > 0) {
				osw = new OutputStreamWriter(con.getOutputStream(), charset);
				osw.write(sbParams.substring(0, sbParams.length() - 1));
				osw.flush();
			}
			// ��ȡ��������
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Map<String, List<String>> headmsgstr=con.getHeaderFields();
				JSONObject itemJSONObj = JSONObject.parseObject(JSON.toJSONString(headmsgstr));
				resultBuffer.append(Constants.RESPONSE_HEAD+itemJSONObj+Constants.RESPONSE_END);
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+con.getResponseCode()+Constants.RESPONSE_END);
			}
			if (null != con.getHeaderField("Content-Length") || null != con.getHeaderField("Transfer-Encoding")) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
				String temp;
				while ((temp = br.readLine()) != null) {
					resultBuffer.append(temp);
				}
			}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpURLConnection����delete��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (osw != null) {
				try {
					osw.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpURLConnection����delete�����ر�osw�������쳣�����飡", e);
					osw = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpURLConnection����delete�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				} finally {
					if (con != null) {
						con.disconnect();
						con = null;
					}
				}
			}
		}

		return resultBuffer.toString();
	}


	/**
	 * ʹ��HttpClient����put����  ����JSON��ʽ
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static String httpClientPutJson(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws KeyManagementException, NoSuchAlgorithmException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		HttpPut httpput = new HttpPut(urlParam);
	    httpput.setHeader("Content-Type", "application/json");
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout)  
	            //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build();  
	    httpput.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻HTTPͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpput.setHeader(key,value);
	    	}else{
	    		httpput.setHeader(key,value);
	    	}
        }
		// �����������
		BufferedReader br = null;
		try {
		if(params.size()>0){
			if(1==params.size()&&params.containsKey("_forTextJson")){
				LogUtil.APP.info("�������ͣ�TEXT,����HTTPClientPutJson������Ϣ...��{}��",params.get("_forTextJson").toString());
				StringEntity entity = new StringEntity(params.get("_forTextJson").toString(),charset);
				httpput.setEntity(entity);
			}else{
			    String jsonString = JSON.toJSONString(params);
				LogUtil.APP.info("�������ͣ�FORM,����HTTPClientPutJson������Ϣ...��{}��",jsonString);
				StringEntity entity = new StringEntity(jsonString,charset);
				httpput.setEntity(entity);
			}

		}
       
		 CloseableHttpResponse response = httpclient.execute(httpput);

			// ��ȡ��������Ӧ����
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Header[] headmsgstr=response.getAllHeaders();
				resultBuffer.append("RESPONSE_HEAD:��{");
				for(Header header:headmsgstr){
					resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
				}
				resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
			}
	        if(null!=response.getEntity()){
	        	br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
				String temp;
				while ((temp = br.readLine()) != null) {
					resultBuffer.append(temp);
				}
	        }
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient����put����(����JSON��ʽ)�����쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient����put����(����JSON��ʽ)��ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}		
		return resultBuffer.toString();
	}

	/**
	 * ʹ��HttpClient����put����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static String httpClientPut(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws KeyManagementException, NoSuchAlgorithmException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		HttpPut httpput = new HttpPut(urlParam);
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout)  
	            //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build();  
	    httpput.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻HTTPͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpput.setHeader(key,value);
	    	}else{
	    		httpput.setHeader(key,value);
	    	}
        }
		// �����������
		BufferedReader br = null;
		try {
			if(params.size()>0){
				if(1==params.size()&&params.containsKey("_forTextJson")){
					LogUtil.APP.warn("Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientPut����(����Ϊkey-value)...");
					return "Э��ģ���Ǵ��ı�ģʽ(����httpClientPostJson�Լ�httpClientPutJson����)���޷�ʹ��httpClientPut����(����Ϊkey-value)...";
				}else{
					//ƴ�Ӳ���
				    List <NameValuePair> nvps = new ArrayList <NameValuePair>();
				    for (Map.Entry<String, Object> m :params.entrySet())  { 
			            nvps.add(new BasicNameValuePair(m.getKey(), m.getValue().toString()));
			            LogUtil.APP.info("��ʼ����HTTPClientPut������Ϣ...key:��{}��    value:��{}��",m.getKey(),m.getValue());
			        }
				    httpput.setEntity(new UrlEncodedFormEntity(nvps,charset));
				}

			}
       
		 CloseableHttpResponse response = httpclient.execute(httpput);

			// ��ȡ��������Ӧ����
			resultBuffer = new StringBuffer();
			if(1==responsehead){
				Header[] headmsgstr=response.getAllHeaders();
				resultBuffer.append("RESPONSE_HEAD:��{");
				for(Header header:headmsgstr){
					resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
				}
				resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
			}
			if(1==responsecode){
				resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
			}
	        if(null!=response.getEntity()){
	        	br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
				String temp;
				while ((temp = br.readLine()) != null) {
					resultBuffer.append(temp);
				}
	        }		
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient����put��������쳣�����飡", e);
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("ʹ��HttpClient����put�����ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}		
		return resultBuffer.toString();
	}

    /**
     * ����������ǩ��֤��
     * @param keyStorePath
     * @param keyStorepass
     * @return
     */
    private static SSLContext sslContextKeyStore(String keyStorePath, String keyStorepass) {
        SSLContext sslContext = null;
        FileInputStream instream = null;
        KeyStore trustStore = null;
        LogUtil.APP.info("֤��·��:{}  ��Կ:{}",keyStorePath,keyStorepass);
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            LogUtil.APP.info("��ʼ��ȡ֤���ļ���...");
            instream = new FileInputStream(new File(keyStorePath));
            LogUtil.APP.info("��ʼ����֤���Լ���Կ...");
            trustStore.load(instream, keyStorepass.toCharArray());
            // �����Լ���CA��������ǩ����֤��
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
            // ���� javax.net.ssl.TrustManager ����
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
            tmf.init(trustStore);
            TrustManager[] tms = tmf.getTrustManagers();
            // ʹ�ù���õ� TrustManager ������Ӧ�� https վ��
            sslContext = SSLContext.getInstance("SSL", "SunJSSE");
            sslContext.init(null, tms, new java.security.SecureRandom());
        } catch (Exception e) {
        	LogUtil.APP.error("����������ǩ��֤������쳣�����飡", e);
        } finally {
            try {
                instream.close();
            } catch (IOException e) {
            	LogUtil.APP.error("����������ǩ��֤���ر�instream�������쳣�����飡", e);
            }
        }
        return sslContext;
    }

    /**
     * httpclient��ʽ HTTP/HTTPS��ʼ��
     * @param urlParam
     * @param cerpath
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static CloseableHttpClient iniHttpClient(String urlParam,String cerpath) throws NoSuchAlgorithmException, KeyManagementException{
    	CloseableHttpClient httpclient=null;
    	urlParam=urlParam.trim();
    	if(urlParam.startsWith("http://")){
    		httpclient = HttpClients.createDefault();
    	}else if(urlParam.startsWith("https://")){
    		//�����ƹ���֤�ķ�ʽ����https����
    		SSLContext sslContext=null;
    		if(null==cerpath||"".equals(cerpath.trim())){
    			LogUtil.APP.info("��ʼ����HTTPS������֤����...");
    	        TrustManager[] trustManagers = {new MyX509TrustManager()};  
    	        sslContext = SSLContext.getInstance("TLS");
    	        sslContext.init(null, trustManagers, new SecureRandom());
    		}else{
    			LogUtil.APP.info("��ʼ����HTTPS˫����֤����...");
    			String[] strcerpath=cerpath.split(";",-1);
    			sslContext = sslContextKeyStore(strcerpath[0], strcerpath[1]);
    		}
            
            // ����Э��http��https��Ӧ�Ĵ���socket���ӹ����Ķ���
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                // ������SSL���ӻ���֤����֤����Ϣ
                // .register("https", new SSLConnectionSocketFactory(sslContext)).build();
                // ����������֤
                .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                .build();
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connManager.setDefaultMaxPerRoute(1);
            //�����Զ����httpclient����
            httpclient = HttpClients.custom().setConnectionManager(connManager).build();
    	}else{
    		httpclient = HttpClients.createDefault();
    	}
    	return httpclient;
    }
	
    /**
	 * ʹ��HttpClient��XML����post����
	 * @param urlParam
	 * @param params
	 * @param charset
	 * @param headmsg
	 * @param cerpath
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static String httpClientPostXml(String urlParam, Map<String, Object> params, Map<String, String> headmsg,ProjectProtocolTemplate ppt) throws NoSuchAlgorithmException, KeyManagementException {
		String cerpath=ppt.getCerificatePath();
		String charset=ppt.getEncoding().toLowerCase();
		int timeout=ppt.getTimeout()*1000;
		int responsehead=ppt.getIsResponseHead();
		int responsecode=ppt.getIsResponseCode();
		
		StringBuffer resultBuffer = null;
		LogUtil.APP.info("����HTTP�����ַ:��{}��",urlParam);
		CloseableHttpClient httpclient=iniHttpClient(urlParam,cerpath);
		HttpPost httpPost = new HttpPost(urlParam);
	    httpPost.setHeader("Content-Type", "text/xml");
	    RequestConfig requestConfig = RequestConfig.custom()  
	            .setConnectTimeout(timeout)
	            .setConnectionRequestTimeout(timeout)  
	             //��������ʹ��䳬ʱʱ��
	            .setSocketTimeout(timeout).build();  
	    httpPost.setConfig(requestConfig);
		//�滻ͷ����Ϣ
	    for (Map.Entry<String, String> m :headmsg.entrySet())  {
	    	String key=m.getKey();
	    	String value=m.getValue();
	    	LogUtil.APP.info("��ʼ����|�滻httpClientPostXmlͷ����Ϣ...key:��{}��    value:��{}��",key,value);
	    	if(null!=value&&value.indexOf("Base64(")==0){
	    		String valuesub=value.substring(value.indexOf("Base64(")+7,value.lastIndexOf(")"));
	    		value="Basic " + DatatypeConverter.printBase64Binary((valuesub).getBytes());
	    		LogUtil.APP.info("��ͷ��{}����ֵ��{}��FORMAT��BASE64��ʽ...",key,value);
	    		httpPost.setHeader(key, value);
	    	}else{
	    		httpPost.setHeader(key, value);
	    	}
        }

		try {
		if(params.size()>0){
			if(1==params.size()&&params.containsKey("_forTextXml")){
				LogUtil.APP.info("�������ͣ�XML,����httpClientPostXml������Ϣ...��{}��",params.get("_forTextXml").toString());
				String xmlStr=getXmlString(params.get("_forTextXml").toString());
				StringEntity entity = new StringEntity(xmlStr,charset);
				entity.setContentType("text/xml");
				httpPost.setEntity(entity);
			}else{
			    String jsonString = JSON.toJSONString(params);
				LogUtil.APP.info("�������ͣ�FORM,����httpClientPostXml������Ϣ...��{}��",jsonString);
				StringEntity entity = new StringEntity(jsonString,charset);
				httpPost.setEntity(entity);
			}
		}
       
		 CloseableHttpResponse response = httpclient.execute(httpPost);

		// ��ȡ��������Ӧ����
		resultBuffer = new StringBuffer();
		if(1==responsehead){
			Header[] headmsgstr=response.getAllHeaders();
			resultBuffer.append("RESPONSE_HEAD:��{");
			for(Header header:headmsgstr){
				resultBuffer.append("\""+header.getName()+"\":\""+header.getValue()+"\",");
			}
			resultBuffer.delete(resultBuffer.length()-1, resultBuffer.length()).append("}�� ");
		}
		if(1==responsecode){
			resultBuffer.append(Constants.RESPONSE_CODE+response.getStatusLine().getStatusCode()+Constants.RESPONSE_END);
		}
		if(null!=response.getEntity()){
            HttpEntity entity =  response.getEntity();
            resultBuffer.append(EntityUtils.toString(entity));
		}
		} catch (Exception e) {
			LogUtil.APP.error("ʹ��HttpClient��XML����post��������쳣�����飡", e);
			throw new RuntimeException(e);
		}	
		return resultBuffer.toString();
	}
	
	/**
	 * ��ȡxml����,�������xml������ַ��� ����post����
	 * @param path
	 * @return
	 * @author Seagull
	 * @date 2019��9��17��
	 */
	private static String getXmlString(String path) {
		StringBuilder sb = new StringBuilder();
		// �����������
	    BufferedReader br = null;
		try {
			InputStream inputStream = new FileInputStream(path);
			br = new BufferedReader(new InputStreamReader(inputStream));
			String line = "";
			for (line = br.readLine(); line != null; line = br.readLine()) {
				sb.append(line + "\n");
			}
		} catch (FileNotFoundException e) {
			LogUtil.APP.error("��·����"+path+"����û���ҵ���Ӧ��XML�ļ�", e);
		} catch (IOException e) {
			LogUtil.APP.error("��ȡ�ļ�ʧ�ܣ������쳣��", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.APP.error("��ȡxml�ļ���ر�br�������쳣�����飡", e);
					br = null;
					throw new RuntimeException(e);
				}
			}
		}	
		return sb.toString();
	}

}