org.json.JSONObject tx = new org.json.JSONObject(input);
return "http://internal-fraud-ml-service/score?userId=" + tx.getString("userId") + "&amount=" + tx.getDouble("amount");
