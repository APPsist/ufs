package de.appsist.service.ufs;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import de.appsist.commons.event.ProcessCompleteEvent;
import de.appsist.commons.util.EventUtil;
import de.appsist.service.iid.server.model.ContentBody;
import de.appsist.service.iid.server.model.Popup;
import de.appsist.service.iid.server.model.PopupBuilder;

public class EventBusHandler {
	private static final Logger logger = LoggerFactory.getLogger(EventBusHandler.class);
	
	private final HandlerRegistry handlers;
	private final ConnectorRegistry connectors;
	private final EventBus eventBus;
	
	public EventBusHandler(HandlerRegistry handlers) {
		this.handlers = handlers;
		this.connectors = handlers.connectors();
		this.eventBus = handlers.eventBus();
		registerPlattformEvents();
		registerServiceMessages();
	}
	
	private void registerServiceMessages() {
		
	}
	
	private void registerPlattformEvents() {
		eventBus.registerHandler("appsist:event:processEvent:processComplete", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonObject body = message.body();
				ProcessCompleteEvent event = EventUtil.parseEvent(body.toMap(), ProcessCompleteEvent.class);
				handleProcessCompleteEvent(event);
			}
		});
		eventBus.registerHandler("appsist:item:interactedWith", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				JsonObject body = message.body();
				handleInteractedWithEvent(body);
			}
		});
	}
	
	private void handleProcessCompleteEvent(final ProcessCompleteEvent event) {
		logger.debug("Received ProcessCompleteEvent: " + new JsonObject(event.asMap()).toString());
		if (event.getParentInstance() != null) {
			// We only want feedback for measures.
			return;
		}
		
		String path = new StringBuilder()
			.append(handlers.config().webserver.basePath)
			.append("/feedbackForm?sid=").append(event.getSessionId())
			.append("&uid=").append(event.getUserId())
			.append("&pid=").append(event.getProcessId())
			.toString();
		
		final Popup popup = new PopupBuilder()
			.setTitle("Feedback")
			.setBody(new ContentBody.Frame(path))
			.build();
		
		// Wait a few seconds for IID to settle.
		handlers.vertx().setTimer(7000, new Handler<Long>() {
			
			@Override
			public void handle(Long timerId) {
				connectors.iid().displayPopup(event.getSessionId(), null, MainVerticle.SERVICE_ID, popup, new AsyncResultHandler<Void>() {
					
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.failed()) {
							logger.warn("Failed to open feedback popup.", event.cause());
						}
					}
				});
			}
		});
	}
	
	private void handleInteractedWithEvent(JsonObject event) {
		logger.debug(">> Received InteractedWithEvent: " + event.toString());
	}
		
	/*
	private static JsonObject createErrorResponse(int code, String message) {
		JsonObject response = new JsonObject()
			.putString("status", "error")
			.putNumber("code", code)
			.putString("message", message);
		return response;
	}
	
	private static JsonObject createOkResponse() {
		JsonObject response = new JsonObject()
			.putString("status", "ok");
		return response;
	}
	*/
}
