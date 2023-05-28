package de.joerghoh.aem.httpclient.impl;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import de.joerghoh.aem.httpclient.RemoteServerErrorException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junit.framework.Assert;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@WireMockTest
@ExtendWith(AemContextExtension.class)
public class HttpClientImplTest {
	
	public final AemContext context = new AemContext();
	
	public final static Map<String,Object> NO_OSGI_CONFIG = Collections.emptyMap();
	
	
	@Test
	protected void successfulCallMustInvokeSuccessHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		
		
		stubFor(get("/returns200").willReturn(ok().withBody("ok")));
		stubFor(get("/return500").willReturn(WireMock.notFound().withBody("not found")));

		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/returns200";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		
		Consumer<SimpleHttpResponse> success = (response) -> { 
			assertEquals("ok",response.getBodyText()); 
			assertEquals(200,response.getCode());
		};
		Consumer<Throwable> failed = (throwable) -> { Assertions.fail("a succesful call must not invoke the fail handler"); };
		
		HttpClientImpl client = getClient(NO_OSGI_CONFIG);
		client.performRequest( request, success, failed);
		
	}
	
	@Test
	protected void internalServerErrorCallsFailedHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		
		stubFor(get("/return500").willReturn(
				aResponse()
					.withStatus(500)
					.withBody("internal server error")
				));
		
		Consumer<SimpleHttpResponse> success = (response) -> { 
			Assertions.fail("an internal server error must invoke the success handler");
		};
		Consumer<Throwable> failed = (throwable) -> { 
			assertTrue(throwable instanceof RemoteServerErrorException);
			RemoteServerErrorException ex = (RemoteServerErrorException) throwable;
			assertEquals(500,ex.getErrorCode());
			
		};
		
		HttpClientImpl client = getClient(NO_OSGI_CONFIG);
		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/return500";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		client.performRequest(request, success, failed);
	}
	
	@Test
	protected void timingOutResponseInvokesFailedHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get("/delay").willReturn(
				aResponse()
					.withStatus(200)
					.withBody("delayed response")
					.withFixedDelay(2000)
				));
		
		Consumer<SimpleHttpResponse> success = (response) -> { 
			Assertions.fail("a timeout must not invoke the success handler");
		};
		Consumer<Throwable> failed = (throwable) -> {
			assertEquals(SocketTimeoutException.class,throwable.getClass());
		};
		
		Map<String,Object> osgiConfig = Map.of(
				"connectionTimeoutInMilis",1
				);
		HttpClientImpl client = getClient(osgiConfig);
		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/delay";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		client.performRequest(request, success, failed);
	}
	
	
	private HttpClientImpl getClient(Map<String,Object> osgiConfigParams) {
		HttpClientImpl client = new HttpClientImpl();
		context.registerInjectActivateService(client, osgiConfigParams);
		
		return client;
	}
	

}
