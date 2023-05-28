package de.joerghoh.aem.httpclient;

import java.util.function.Consumer;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

public interface SimpleHttpClient {

	
	public void performRequest(SimpleHttpRequest request,Consumer<SimpleHttpResponse> success, Consumer<Throwable> failed);
	
	
}
