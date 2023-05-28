package de.joerghoh.aem.httpclient.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.io.entity.EntityUtils;
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
	
	
	protected CloseableHttpAsyncClient httpclient;
	

	@Override
	public void performRequest(SimpleHttpRequest request,Consumer<SimpleHttpResponse> success, Consumer<Throwable> failed)  {
		
        final Future<SimpleHttpResponse> future = httpclient.execute(
                SimpleRequestProducer.create(request),
                SimpleResponseConsumer.create(),
                new FutureCallback<SimpleHttpResponse>() {

                    @Override
                    public void completed(final SimpleHttpResponse response) {
                    	if (response.getCode() >= 500) {
                    		String msg;
                    		int errorCode = response.getCode();
							try {
								msg = "Request to " + request.getUri().toString() + " returned with statuscode " + errorCode;
							} catch (URISyntaxException e) {
								msg = "Remote request returned with statuscode " + errorCode;
							}
                    		RemoteServerErrorException ex = new RemoteServerErrorException(msg, errorCode);
                    		failed.accept(ex);
                    	} else {
                    		success.accept(response);
                    	}
                    }

                    @Override
                    public void failed(final Exception ex) {
                        failed.accept(ex);
                    }

                    @Override
                    public void cancelled() {
                        // this should not happen in this case, as this request is executed synchronously
                    }

                });
        try {
        	future.get();
        } catch (Exception e) {
        	failed.accept(e.getCause());
        }
	}
	
	
	
	
	
	@Activate
	public void activate(HttpClientConfiguration config) {
		
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
	}
	
	
	@Deactivate
	public void deactivate() throws IOException {
		httpclient.close();
	}
	

}
