package de.appsist.service.ufs;

import org.vertx.java.core.json.JsonObject;

import de.appsist.commons.misc.StatusSignalConfiguration;

public class ModuleConfiguration {
	public static class Webserver {
		public final int port;
		public final String basePath;
		
		private Webserver(JsonObject json) throws IllegalArgumentException {
			
			if (json == null) {
				throw new IllegalArgumentException("Missing [webserver] configuration.");
			}
			
			port = json.getInteger("port");
			if (port == 0) {
				throw new IllegalArgumentException("Missing or invalid [webserver.port].");
			}
			
			basePath = json.getString("basePath");
			if (basePath == null || basePath.isEmpty()) {
				throw new IllegalArgumentException("Missing or invalid [webserver.basePath].");
			}
		}
	}
	
	private final JsonObject json;
	
	public final Webserver webserver;
	public final String mongoPersistorAddress;
	public final boolean debugMode;
	public final StatusSignalConfiguration statusSignalConfig;
	public final JsonObject services;
	
	public ModuleConfiguration(JsonObject json) throws IllegalArgumentException {
		if (json == null) {
			throw new IllegalArgumentException("Missing service configuration."); 
		}
		
		this.json = json;
		webserver = new Webserver(json.getObject("webserver"));
		mongoPersistorAddress = json.getString("mongoPersistorAddress");
		if (mongoPersistorAddress == null) {
			throw new IllegalArgumentException("Missing mongo persistor address [mongoPersistorAddress] string.");
		}
		debugMode = json.getBoolean("debugMode", false);
		JsonObject statusSignalObject = json.getObject("statusSignal");
		statusSignalConfig = statusSignalObject != null ? new StatusSignalConfiguration(statusSignalObject) : new StatusSignalConfiguration();
		services = json.getObject("services");
	}
	
	public JsonObject getJson() {
		return json;
	}
}
