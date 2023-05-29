package de.joerghoh.aem.httpclient;

import java.util.function.Function;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

public interface SimpleHttpClient {

	
	public <R> R performRequest(SimpleHttpRequest request,Function<SimpleHttpResponse,R> success, Function<Throwable,R> failed);
	
	
}
