String[] parts = input.split("-");
long index = Long.parseLong(parts[parts.length - 1]);
String userId = "USER_" + String.format("%03d", index % 1000);
double amount = Math.round((10.0 + Math.random() * 4990.0) * 100.0) / 100.0;
String[] locations = {"NYC", "LAX", "CHI", "HOU", "PHX"};

org.json.JSONObject tx = new org.json.JSONObject();
tx.put("userId", userId);
tx.put("amount", amount);
tx.put("location", locations[(int)(index % 5)]);
tx.put("timestamp", java.time.Instant.now().toString());
return tx.toString();
