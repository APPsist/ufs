package de.appsist.service.ufs;

import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import org.vertx.java.core.json.JsonObject;

/**
 * Model for feedback items.
 * @author simon.schwantzer(at)im-c.de
 */
public class Feedback {
	
	private final JsonObject json;
	
	public Feedback(JsonObject json) {
		this.json = json;
	}
	
	public Feedback(String id) {
		this.json = new JsonObject();
		json.putString("id", id);
		json.putString("created", DatatypeConverter.printDateTime(Calendar.getInstance()));
	}
	
	public JsonObject asJson() {
		return json;
	}
	
	public String getId() {
		return json.getString("id");
	}
	
	public String getCreated() {
		return json.getString("created");
	}
	
	public void setProcessId(String processId) {
		json.putString("processId", processId);
	}
	
	public String getProcessId() {
		return json.getString("processId");
	}
	
	public void setRating(int rating) {
		json.putNumber("rating", rating);
	}
	
	public int getRating() {
		return json.getInteger("rating", 0);
	}
	
	public void setContentFlaw(boolean hasContentFlaw) {
		json.putBoolean("hasContentFlaw", hasContentFlaw);
	}
	
	public boolean hasContentFlaw() {
		return json.getBoolean("hasContentFlaw", false);
	}
	
	public void setContentFlawDetails(String text) {
		json.putString("contentFlawDetails", text);
	}
	
	public String getContentFlawDetails() {
		return json.getString("contentFlawDetails");
	}
	
	public void setTechnicalFlaw(boolean hasTechnicalFlaw) {
		json.putBoolean("hasTechnicalFlaw", hasTechnicalFlaw);
	}
	
	public boolean hasTechnicalFlaw() {
		return json.getBoolean("hasTechnicalFlaw", false);
	}
	
	public void setTechnicalFlawDetails(String text) {
		json.putString("technicalFlawDetails", text);
	}
	
	public String getTechnicalFlawDetails() {
		return json.getString("technicalFlawDetails");
	}
	
	public void setMiscFeedback(boolean hasMiscFeedback) {
		json.putBoolean("hasMiscFeedback", hasMiscFeedback);
	}
	
	public boolean hasMiscFeedback() {
		return json.getBoolean("hasMiscFeedback", false);
	}
	
	public void setMiscFeedbackDetails(String text) {
		json.putString("miscFeedbackDetails", text);
	}
	
	public String getMiscFeedbackDetails() {
		return json.getString("miscFeedbackDetails");
	}
	
	public void setUserId(String userId) {
		json.putString("userId", userId);
	}
	
	public String getUserId() {
		return json.getString("userId");
	}
}
