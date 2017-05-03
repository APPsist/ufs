package de.appsist.service.ufs;

import org.vertx.java.core.Vertx;

import de.appsist.service.auth.connector.AuthServiceConnector;
import de.appsist.service.iid.server.connector.IIDConnector;
import de.appsist.service.pki.connector.PKIConnector;
import de.appsist.service.ufs.connector.MongoDBConnector;

public class ConnectorRegistry {
	
	private final MongoDBConnector mongoDbConnector;
	private final AuthServiceConnector authServiceConnector;
	private final IIDConnector iidConnector;
	private final PKIConnector pkiConnector;
	
	public ConnectorRegistry(Vertx vertx, ModuleConfiguration config) {
		mongoDbConnector = new MongoDBConnector(config.mongoPersistorAddress, vertx.eventBus());
		authServiceConnector = new AuthServiceConnector(vertx.eventBus(), AuthServiceConnector.SERVICE_ID);
		iidConnector = new IIDConnector(vertx.eventBus(), IIDConnector.DEFAULT_ADDRESS);
		pkiConnector = new PKIConnector(
				vertx,
				config.services.getString("host"),
				config.services.getInteger("port"),
				config.services.getBoolean("secure"),
				config.services.getObject("paths").getString("pki"));
	}
	
	public MongoDBConnector mongo() {
		return mongoDbConnector;
	}
	
	public AuthServiceConnector auth() {
		return authServiceConnector;
	}

	public IIDConnector iid() {
		return iidConnector;
	}
	
	public PKIConnector pki() {
		return pkiConnector;
	}
}
