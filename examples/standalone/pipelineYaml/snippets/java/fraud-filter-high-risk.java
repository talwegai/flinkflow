org.json.JSONObject event = new org.json.JSONObject(input);
double riskScore = event.getDouble("fraudScore");
if (riskScore > 0.85) {
    event.put("alertTime", java.time.Instant.now().toString());
    return event.toString();
} else {
    return null;
}
