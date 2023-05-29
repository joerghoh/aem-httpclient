package de.joerghoh.aem.httpclient.impl;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.core5.util.DeadlineTimeoutException;
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
	
	public final static Map<String,Object> DEFAULT_OSGI_CONFIG = Collections.emptyMap();
	
	public final static String EXPECTED_RESULT = "expected";
	public final static String UNEXPECTED_RESULT = "fail";
	
	
	@Test
	protected void successfulCallMustInvokeSuccessHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		
		
		stubFor(get("/returns200").willReturn(ok().withBody("ok")));

		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/returns200";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		
		Function<SimpleHttpResponse,String> success = (response) -> { 
			assertEquals("ok",response.getBodyText()); 
			assertEquals(200,response.getCode());
			return "ok";
		};
		Function<Throwable,String> failed = (throwable) -> { 
				Assertions.fail("a succesful call must not invoke the fail handler"); 
				return "failed";
			};
		
		HttpClientImpl client = getClient(DEFAULT_OSGI_CONFIG);
		assertEquals("ok",client.performRequest( request, success, failed));
		
	}
	
	@Test
	protected void internalServerErrorCallsFailedHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		
		stubFor(get("/return500").willReturn(
				aResponse()
					.withStatus(500)
					.withBody("internal server error")
				));
		
		Function<SimpleHttpResponse,String> success = (response) -> { 
			Assertions.fail("an internal server error must invoke the success handler");
			return "failed";
		};
		Function<Throwable,String> failed = (throwable) -> { 
			assertTrue(throwable instanceof RemoteServerErrorException);
			RemoteServerErrorException ex = (RemoteServerErrorException) throwable;
			assertEquals(500,ex.getErrorCode());
			return "ok";
		};
		
		HttpClientImpl client = getClient(DEFAULT_OSGI_CONFIG);
		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/return500";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		assertEquals("ok",client.performRequest(request, success, failed));
	}
	
	@Test
	protected void timingOutResponseInvokesFailedHandler(WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get("/delay").willReturn(
				aResponse()
					.withStatus(200)
					.withBody("delayed response")
					.withFixedDelay(2000)
				));
		
		Function<SimpleHttpResponse,String> success = (response) -> { 
			Assertions.fail("a timeout must not invoke the success handler");
			return "failed";
		};
		Function<Throwable,String> failed = (throwable) -> {
			assertEquals(SocketTimeoutException.class,throwable.getClass());
			return "ok";
		};
		
		Map<String,Object> osgiConfig = Map.of(
				"connectionTimeoutInMilis",1
				);
		HttpClientImpl client = getClient(osgiConfig);
		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/delay";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		assertEquals("ok",client.performRequest(request, success, failed));
	}
	
	@Test
	protected void nonAvailableConnectionTriggersFailure(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException, ExecutionException {
		// test the connection request timeout -- we cannot get a connection in time from the pool
		stubFor(get("/slow").willReturn(
				aResponse()
					.withStatus(200)
					.withBody("delayed response")
					.withFixedDelay(2000)
				));
		
		Function<SimpleHttpResponse,String> success1 = (response) -> { 
			return EXPECTED_RESULT;
		};
		Function<Throwable,String> failed1 = (throwable) -> {
			Assertions.fail("it is just slow, but must not invoke a failure!");
			return UNEXPECTED_RESULT;
		};
		
		Function<SimpleHttpResponse,String> success2 = (response) -> {
			Assertions.fail("this should have never get invoked");
			return UNEXPECTED_RESULT;
		};
		Function<Throwable,String> failed2 = (throwable) -> {
			assertEquals(DeadlineTimeoutException.class,throwable.getClass());
			return EXPECTED_RESULT;
		};
		
		// allow just 1 request to go to the server at once, and it should be 
		// successful, so we increase the timeout to be higher than the delay
		Map<String,Object> osgiConfig = Map.of(
				"maxConnectionsPerRoute",1,
				"connectionTimeoutInMilis",10000
				);
		HttpClientImpl client = getClient(osgiConfig); 
		String requestPath = wmRuntimeInfo.getHttpBaseUrl() + "/slow";
		SimpleHttpRequest request = SimpleRequestBuilder.get(requestPath).build();
		
		
		Runnable successButSlow = () -> {
			assertEquals(EXPECTED_RESULT,client.performRequest(request, success1, failed1));
		};
		Runnable willFailWithTimeout = () -> {
			assertEquals(EXPECTED_RESULT,client.performRequest(request, success2, failed2));
		};
		
		final ExecutorService pool = Executors.newFixedThreadPool(5);
		Future<?> f1 = pool.submit(successButSlow);
		Thread.sleep(100);
		Future<?> f2 = pool.submit(willFailWithTimeout);
		
		f1.get();
		f2.get();
	}
	
	
	
	
	private HttpClientImpl getClient(Map<String,Object> osgiConfigParams) {
		HttpClientImpl client = new HttpClientImpl();
		context.registerInjectActivateService(client, osgiConfigParams);
		
		return client;
	}
	

}
