com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.JsonNode obj = mapper.readTree(input);
String status = obj.get("temperature").asDouble() > 30 ? "CRITICAL" : "NORMAL";
return "Truck " + obj.get("truckId").asText() + " is " + status + " (" + obj.get("temperature").asDouble() + "C)";
