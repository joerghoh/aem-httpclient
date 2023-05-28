package de.joerghoh.aem.httpclient.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface HttpClientConfiguration {
	
	public static final int DEFAULT_SOCKET_TIMEOUT_IN_MILIS = 1000;
	
	public static final int DEFAULT_CONNECTION_TIMEOUT_IN_MILIS = 1000;
	
	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_IN_MILIS = 500;
	
	public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE=10;
	
	public static final int DEFAULT_MAX_CONNECTIONS=20;
	
	
	
	@AttributeDefinition(name = "id", description="name through which this instance is referenced")
	public String id() default "default";
	
	
	@AttributeDefinition(name = "Socket Timeout in miliseconds (time waiting for data when connection is established)")
	public int socketTimeoutInMilis() default DEFAULT_SOCKET_TIMEOUT_IN_MILIS;
	
	@AttributeDefinition(name = "connection timeout in miliseconds (time to wait to establish a connection)")
	public int connectionTimeoutInMilis() default DEFAULT_CONNECTION_TIMEOUT_IN_MILIS;
	
	@AttributeDefinition(name = "connection request timeout in miliseconds (when waiting for a connection from the pool")
	public int connectionRequestTimeoutInMilis() default DEFAULT_CONNECTION_REQUEST_TIMEOUT_IN_MILIS;
	
	@AttributeDefinition(name = "Use system properties")
	public boolean useSystemProperties() default true;
	
	@AttributeDefinition(name="Max connections per route")
	public int maxConnectionsPerRoute() default DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
	
	
	@AttributeDefinition(name="Max total connections")
	public int maxConnections() default DEFAULT_MAX_CONNECTIONS;

}
