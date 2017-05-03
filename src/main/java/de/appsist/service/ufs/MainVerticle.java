package de.appsist.service.ufs;

import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Verticle;

import de.appsist.commons.misc.StatusSignalSender;

/**
 * Main verticle for the user feedback module.
 * @author simon.schwantzer(at)im-c.de
 */
public class MainVerticle extends Verticle {
	private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
	public static final String SERVICE_ID = "appsist:service:ufs";
	
	private ModuleConfiguration config;
	private RouteMatcher routeMatcher;
	private HandlerRegistry handlers;
	private ConnectorRegistry connectors;

	@Override
	public void start() {
		try {
			config = new ModuleConfiguration(container.config());
		} catch (IllegalArgumentException e) {
			logger.error("Missing or invalid configuration!", e);
			System.exit(1);
		}
		
		connectors = new ConnectorRegistry(vertx, config);
		
		handlers = new HandlerRegistry(vertx, connectors, config);
		handlers.init();
		
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.requestHandler(routeMatcher);
		
		container.logger().info("APPsist User Feedback Service has been initialized.");
		StatusSignalSender statusSignalSender = new StatusSignalSender("ufs", vertx, config.statusSignalConfig);
		statusSignalSender.start();
	}
	
	@Override
	public void stop() {
		container.logger().info("APPsist User Feedback Service has been stopped.");
	}
}
