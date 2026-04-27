org.json.JSONObject tx = new org.json.JSONObject(input);
double score = Math.round(Math.random() * 1000.0) / 1000.0;
tx.put("fraudScore", score);
return tx.toString();
