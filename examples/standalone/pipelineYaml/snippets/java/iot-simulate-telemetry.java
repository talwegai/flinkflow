String[] parts = input.split("-");
long index = Long.parseLong(parts[parts.length - 1]);

String[] trucks = {"TRUCK_01", "TRUCK_02", "TRUCK_03"};
String truckId = trucks[(int)(index % 3)];

double temp = -18.0 + (Math.random() * 45.0);

com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.node.ObjectNode payload = mapper.createObjectNode();
payload.put("truckId", truckId);
payload.put("temperature", Math.round(temp * 100.0) / 100.0);
payload.put("timestamp", java.time.Instant.now().toString());

return payload.toString();
