package de.appsist.service.ufs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import de.appsist.service.pki.model.ProcessDefinition;

public class HttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);
	
	private final RouteMatcher routeMatcher;
	private final String basePath;
	private final Map<String, Template> templates;
	private final ConnectorRegistry connectors;

	public HttpHandler(Vertx vertx, String basePath, int port, ConnectorRegistry connectors) {
		this.basePath = basePath;
		this.connectors = connectors;
		
		HttpServer httpServer = vertx.createHttpServer();
		routeMatcher = new BasePathRouteMatcher(basePath);
		
		templates = new HashMap<>();
		try {
			Handlebars handlebars = new Handlebars();
			templates.put("processFeedbackForm", handlebars.compile("templates/feedbackForm"));
			templates.put("feedbackComplete", handlebars.compile("templates/feedbackComplete"));
			templates.put("noFeedback", handlebars.compile("templates/noFeedback"));
			templates.put("feedbackOverview", handlebars.compile("templates/feedbackOverview"));
			templates.put("feedbackDetails", handlebars.compile("templates/feedbackDetails"));
		} catch (IOException e) {
			logger.fatal("Failed to load templates.", e);
		}
		
		initRouteMatcher();
		httpServer.requestHandler(routeMatcher).listen(port);
	}
	
	private void initRouteMatcher() {
		routeMatcher.get("/feedbackForm", new Handler<HttpServerRequest>() {
				
				@Override
				public void handle(HttpServerRequest request) {
					HttpServerResponse response = request.response();
					String sessionId = request.params().get("sid");
					String userId = request.params().get("uid");
					String processId = request.params().get("pid");
					JsonObject data = new JsonObject()
						.putString("sid", sessionId)
						.putString("uid", userId)
						.putString("pid", processId);
					renderResponse(response, templates.get("processFeedbackForm"), data);
				}
			}).post("/postFeedback", new Handler<HttpServerRequest>() {

				@Override
				public void handle(final HttpServerRequest request) {
					final HttpServerResponse response = request.response();
					request.expectMultiPart(true);
					
					request.endHandler(new Handler<Void>() {
						
						@Override
						public void handle(Void event) {
							storeFeedback(request.formAttributes(), new AsyncResultHandler<Void>() {

								@Override
								public void handle(AsyncResult<Void> event) {
									if (event.failed()) {
										logger.warn("Failed to store feedback.", event.cause());
									}
									renderResponse(response, templates.get("feedbackComplete"), new JsonObject());
								}
							});
						}
					});
					
				}
				
			}).get("/admin/feedbackOverview", new Handler<HttpServerRequest>() {
				
				@Override
				public void handle(HttpServerRequest request) {
					final HttpServerResponse response = request.response();
					final JsonObject data = new JsonObject();
					final String selectedProcessId = request.params().get("pid");
					
					connectors.mongo().find("feedback", new JsonObject(), null, new AsyncResultHandler<JsonArray>() {
						
						@Override
						public void handle(AsyncResult<JsonArray> feedbackRequest) {
							if (feedbackRequest.failed()) {
								// Error retrieving feedback.
								logger.warn("Failed to retrieve user feedback.", feedbackRequest.cause());
								renderResponse(response, templates.get("noFeedback"), new JsonObject());
								return;
							}
							
							final List<Feedback> feedbackList = new ArrayList<>();
							for (Object entry : feedbackRequest.result()) {
								Feedback feedback = new Feedback((JsonObject) entry);
								feedbackList.add(feedback);
							}
							
							if (feedbackList.isEmpty()) {
								// No feedback at all.
								renderResponse(response, templates.get("noFeedback"), new JsonObject());
								return;
							}
							
							Set<String> ratedProcesses = new HashSet<>();
							for (Feedback feedback : feedbackList) {
								ratedProcesses.add(feedback.getProcessId());
							}
							
							ResultAggregationHandler<String, ProcessDefinition> aggregationHandler = new ResultAggregationHandler<String, ProcessDefinition>(ratedProcesses, new AsyncResultHandler<Map<String, AsyncResult<ProcessDefinition>>>() {
							
								@Override
								public void handle(AsyncResult<Map<String, AsyncResult<ProcessDefinition>>> aggregatedResult) {
									Map<String, AsyncResult<ProcessDefinition>> results = aggregatedResult.result();
									Map<String, ProcessDefinition> processDefinitions = new HashMap<>();
									for (String processId : results.keySet()) {
										AsyncResult<ProcessDefinition> result = results.get(processId);
										if (result.succeeded()) {
											processDefinitions.put(processId, result.result());
										} else {
											logger.warn("Failed to retrieve process " + processId + ".", result.cause());
										}	
									}
									data.putArray("processes", encodeProcesses(processDefinitions));
									
									if (selectedProcessId != null && processDefinitions.containsKey(selectedProcessId)) {
										ProcessDefinition selectedProcess = processDefinitions.get(selectedProcessId);
										data.putObject("selectedProcess", new JsonObject()
											.putString("id", selectedProcessId)
											.putString("title", selectedProcess.getLabel()));
									}
									
									JsonObject rating = encodeRating(feedbackList);
									if (rating.getInteger("numTotal") > 0) {
										data.putObject("rating", rating);
									}
									
									JsonObject contentFlaws = encodeContentFlaws(feedbackList);
									if (contentFlaws.getInteger("numTotal") > 0) {
										data.putObject("contentFlaws", contentFlaws);
									}
									
									JsonObject technicalFlaws = encodeTechnicalFlaws(feedbackList);
									if (technicalFlaws.getInteger("numTotal") > 0) {
										data.putObject("technicalFlaws", technicalFlaws);
									}
									
									JsonObject miscFeedback = encodeMiscFeedback(feedbackList);
									if (miscFeedback.getInteger("numTotal") > 0) {
										data.putObject("miscFeedback", miscFeedback);
									}
									renderResponse(response, templates.get("feedbackOverview"), data);
								}
							});
							for (String processId : ratedProcesses) {
								connectors.pki().getProcessDefinition(processId, aggregationHandler.getRequestHandler(processId));
							}
						}
					});
				}
			}).get("/admin/feedbackDetails", new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					final HttpServerResponse response = request.response();
					final String processId = request.params().get("pid");
					final String feedbackId = request.params().get("fid");
					
					connectors.mongo().findOne("feedback", new JsonObject().putString("id", feedbackId), null, new AsyncResultHandler<JsonObject>() {
						
						@Override
						public void handle(AsyncResult<JsonObject> feedbackRequest) {
							if (feedbackRequest.failed()) {
								response.setStatusCode(500).end("Failed to retrieve feedback.");
								return;
							}
							final Feedback feedback = new Feedback(feedbackRequest.result());
							connectors.pki().getProcessDefinition(processId, new AsyncResultHandler<ProcessDefinition>() {
								
								@Override
								public void handle(AsyncResult<ProcessDefinition> processRequest) {
									if (processRequest.failed()) {
										response.setStatusCode(500).end("Failed to retrieve process.");
										return;
									}
									ProcessDefinition process = processRequest.result();
									JsonObject data = new JsonObject();
									
									data.putObject("process", new JsonObject()
											.putString("id", process.getId())
											.putString("title", process.getLabel()));
									
									data.putObject("feedback", feedback.asJson());
									
									renderResponse(response, templates.get("feedbackDetails"), data);
								}
							});
						}
					});
					
				}
			}).getWithRegEx("/.+", new Handler<HttpServerRequest>() {
				
				@Override
				public void handle(HttpServerRequest request) {
					request.response().sendFile("www" + request.path().substring(basePath.length()));
				}
			});
	}

	private void storeFeedback(MultiMap params, final AsyncResultHandler<Void> resultHandler) {
		Feedback feedback = new Feedback(UUID.randomUUID().toString());
		feedback.setProcessId(params.get("pid"));
		
		String rating = params.get("totalRating");
		if (rating != null && !rating.trim().isEmpty()) {
			feedback.setRating(Integer.valueOf(rating));
		}
		
		if (params.contains("contentFlaw")) {
			feedback.setContentFlaw(true);
			String contentFlawDetails = params.get("contentFlawDetails");
			if (contentFlawDetails != null && !contentFlawDetails.trim().isEmpty()) {
				feedback.setContentFlawDetails(contentFlawDetails.trim());
			}
		}
		
		if (params.contains("technicalFlaw")) {
			feedback.setTechnicalFlaw(true);
			String technicalFlawDetails = params.get("technicalFlawDetails");
			if (technicalFlawDetails != null && !technicalFlawDetails.trim().isEmpty()) {
				feedback.setTechnicalFlawDetails(technicalFlawDetails.trim());
			}
		}
		
		if (params.contains("miscFeedback")) {
			feedback.setMiscFeedback(true);
			String miscFeedbackDetails = params.get("miscFeedbackDetails");
			if (miscFeedbackDetails != null && !miscFeedbackDetails.trim().isEmpty()) {
				feedback.setMiscFeedbackDetails(miscFeedbackDetails.trim());
			}
		}
		
		final boolean isPersonalized = params.contains("addUserId");
		if (isPersonalized) {
			String userId = params.get("uid");
			feedback.setUserId(userId);
		}
		connectors.mongo().save("feedback", feedback.asJson(), resultHandler);
	}
	
	private static JsonArray encodeProcesses(Map<String, ProcessDefinition> processes) {
		List<JsonObject> processList = new ArrayList<JsonObject>();
		for (String processId : processes.keySet()) {
			ProcessDefinition process = processes.get(processId);
			JsonObject item = new JsonObject()
				.putString("title", process.getLabel())
				.putString("id", processId);
			processList.add(item);
		}
		Collections.sort(processList, new Comparator<JsonObject>() {

			@Override
			public int compare(JsonObject o1, JsonObject o2) {
				String t1 = (o1 != null) ? o1.getString("title") : null;
				String t2 = (o2 != null) ? o2.getString("title") : null;
				if (t1 == null && t2 == null) return 0;
				if (t1 == null) return -1;
				if (t2 == null) return 1;
				return t1.compareToIgnoreCase(t2);
			}
		});
		JsonArray array = new JsonArray();
		for (JsonObject obj : processList) array.addObject(obj);
		return array;
	}
	
	private static JsonObject encodeRating(List<Feedback> feedbackList) {
		JsonObject json = new JsonObject();
		
		int numTotal = 0;
		
		int sumTotal = 0;
		for (Feedback feedback : feedbackList) {
			int rating = feedback.getRating(); 
			if (rating > 0) {
				numTotal += 1;
				sumTotal += rating;
			}
		}
		json.putNumber("numTotal", numTotal);
		json.putNumber("average", (float) sumTotal / numTotal);
		return json;
	}
	
	private static JsonObject encodeContentFlaws(List<Feedback> feedbackList) {
		JsonObject contentFlaws = new JsonObject();
		
		int numTotal = 0;
		int numDetails = 0;
		JsonArray contentFlawDetails = new JsonArray();
		for (Feedback feedback : feedbackList) {
			if (feedback.hasContentFlaw()) {
				numTotal += 1;
				String details = feedback.getContentFlawDetails();
				if (details != null) {
					numDetails += 1;
					contentFlawDetails.addObject(new JsonObject()
						.putString("id", feedback.getId())
						.putString("text", details));
				}
			}
		}
		contentFlaws.putNumber("numTotal", numTotal);
		contentFlaws.putNumber("numDetails", numDetails);
		if (contentFlawDetails.size() > 0) {
			contentFlaws.putArray("details", contentFlawDetails);
		}
		
		return contentFlaws;
	}
	
	private static JsonObject encodeTechnicalFlaws(List<Feedback> feedbackList) {
		JsonObject technicalFlaws = new JsonObject();
		
		int numTotal = 0;
		int numDetails = 0;
		JsonArray technicalFlawDetails = new JsonArray();
		for (Feedback feedback : feedbackList) {
			if (feedback.hasTechnicalFlaw()) {
				numTotal += 1;
				String details = feedback.getTechnicalFlawDetails();
				if (details != null) {
					numDetails += 1;
					technicalFlawDetails.addObject(new JsonObject()
						.putString("id", feedback.getId())
						.putString("text", details));
				}
			}
		}
		technicalFlaws.putNumber("numTotal", numTotal);
		technicalFlaws.putNumber("numDetails", numDetails);
		if (technicalFlawDetails.size() > 0) {
			technicalFlaws.putArray("details", technicalFlawDetails);
		}
		
		return technicalFlaws;
	}
	
	private static JsonObject encodeMiscFeedback(List<Feedback> feedbackList) {
		JsonObject miscFeedback = new JsonObject();
		
		int numTotal = 0;
		JsonArray miscFeedbackTexts = new JsonArray();
		for (Feedback feedback : feedbackList) {
			if (feedback.hasMiscFeedback()) {
				numTotal += 1;
				String text = feedback.getMiscFeedbackDetails();
				if (text != null) {
					miscFeedbackTexts.addObject(new JsonObject()
						.putString("id", feedback.getId())
						.putString("text", text));
				}
			}
		}
		miscFeedback.putNumber("numTotal", numTotal);
		if (miscFeedbackTexts.size() > 0) {
			miscFeedback.putArray("items", miscFeedbackTexts);
		}
		
		return miscFeedback;
	}
	
	private static void renderResponse(HttpServerResponse response, Template template, JsonObject data) {
		try {
			String html = template.apply(data.toMap());
			response.end(html);
		} catch (IOException e) {
			logger.warn("Failed to apply template.", e);
			response.end("Es ist ein interner Fehler aufgetreten.");
		}
	}
}
