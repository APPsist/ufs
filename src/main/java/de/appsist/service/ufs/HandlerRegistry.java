package de.appsist.service.ufs;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;

public class HandlerRegistry {
	
	private final Vertx vertx;
	private final ConnectorRegistry connectors;
	private final ModuleConfiguration config;
	private EventBusHandler eventBusHandler;
	private HttpHandler httpHandler;
	
	public HandlerRegistry(Vertx vertx, ConnectorRegistry connectors, ModuleConfiguration config) {
		this.vertx = vertx;
		this.connectors = connectors;
		this.config = config;
	}
	
	public void init() {
		eventBusHandler = new EventBusHandler(this); 
		httpHandler = new HttpHandler(vertx, config.webserver.basePath, config.webserver.port, connectors);
	}
	
	public Vertx vertx() {
		return vertx;
	}
	
	public EventBus eventBus() {
		return vertx.eventBus();
	}
	
	public ModuleConfiguration config() {
		return config;
	}
	
	public ConnectorRegistry connectors() {
		return connectors;
	}
	
	public EventBusHandler eventBusHandler() {
		return eventBusHandler;
	}
	
	public HttpHandler httpHandler() {
		return httpHandler;
	}
	
}
