package de.joerghoh.aem.httpclient.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.aem.httpclient.RemoteServerErrorException;
import de.joerghoh.aem.httpclient.SimpleHttpClient;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = HttpClientConfiguration.class,factory = true)
public class HttpClientImpl implements SimpleHttpClient {
	
	// no static logger, will be initialized later
	private Logger logger;
	
	private static final int WARN_AT_BODY_SIZE = 1024*1024; // 1 megabyte;
	
	
	HttpClientConfiguration config;
	
	protected CloseableHttpAsyncClient httpclient;
	

	@Override
	public <R> R performRequest(SimpleHttpRequest request,Function<SimpleHttpResponse,R> success, Function<Throwable,R> failed)  {
		
		String tmp = "";
		try {
			tmp = request.getUri().toString();
		} catch (URISyntaxException ex) {
			tmp = "(invalid uri, exception = " + ex.getMessage() + ")";
		}
		final String requestUri = tmp;
		
        final Future<SimpleHttpResponse> future = httpclient.execute(
                SimpleRequestProducer.create(request),
                SimpleResponseConsumer.create(),
                null);
        try {
        	SimpleHttpResponse response = future.get();
        	int statuscode = response.getCode();
        	if (statuscode >= 500) {
        		String msg = "Request to " + requestUri + " returned with statuscode " + statuscode;
        		RemoteServerErrorException ex = new RemoteServerErrorException(msg, statuscode);
        		return failed.apply(ex);
        	} else {
        		long responseSize = response.getBodyBytes().length;
        		if (responseSize > WARN_AT_BODY_SIZE) {
        			String msg = String.format("the response size for the request [%s] is %s bytes; "
        					+ "please consider to switch to a streaming approach for such requests", 
        					requestUri, responseSize);
        			logger.warn(msg);
        		}
        		return success.apply(response);
        	}
        } catch (ExecutionException ee) {
        	// The executionException wraps the original exception
        	return failed.apply(ee.getCause());
        } catch (InterruptedException ie) {
        	logger.error("InterruptedException while requesting " + requestUri, ie);
        	return failed.apply(ie);
        }
	}
	
	
	
	
	
	@Activate
	public void activate(HttpClientConfiguration config) {
		
		this.config = config;
		
		logger = LoggerFactory.getLogger(HttpClientImpl.class.getName() + "." + config.id());
		
		final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(config.socketTimeoutInMilis()))
                .build();
		
		final RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(config.connectionRequestTimeoutInMilis()))
				.setResponseTimeout(Timeout.ofMilliseconds(config.connectionTimeoutInMilis()))
				.build();
		
		
		PoolingAsyncClientConnectionManager connManager = new PoolingAsyncClientConnectionManager();
		connManager.setDefaultMaxPerRoute(config.maxConnectionsPerRoute());
		connManager.setMaxTotal(config.maxConnections());
		connManager.setDefaultConnectionConfig(ConnectionConfig.custom().
				setConnectTimeout(Timeout.ofMilliseconds(config.connectionTimeoutInMilis()))
				.build());
		
		HttpAsyncClientBuilder builder =  HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig);
		
		if (config.useSystemProperties()) {
			builder = builder.useSystemProperties();
		}
		

		httpclient = builder.build();
        httpclient.start();
        logger.info("started " + this);
	}
	
	
	@Deactivate
	public void deactivate() throws IOException {
		httpclient.close();
	}
	
	
	public String toString() {
		return String.format("aem-httpclient(id=%s,socketTimeOut=%s ms,"
				+ "connectionTimeout=%s ms,connectionRequesttimeout=%s ms,"
				+ "maxConnectionsPerRoute=%s,maxConnections=%s)",
				config.id(),
				config.socketTimeoutInMilis(),
				config.connectionTimeoutInMilis(),
				config.connectionRequestTimeoutInMilis(),
				config.maxConnectionsPerRoute(),
				config.maxConnections()
				);
		
	}
	
	

}
