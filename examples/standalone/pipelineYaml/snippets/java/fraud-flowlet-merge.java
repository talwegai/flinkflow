org.json.JSONObject tx = new org.json.JSONObject(input);
org.json.JSONObject mlResponse = new org.json.JSONObject(response);
tx.put("fraudScore", mlResponse.optDouble("probability_score", 0.0));
return tx.toString();
