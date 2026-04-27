com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(input);
if (obj.get("averageTemperature").asDouble() > 0.0) {
    obj.put("STATUS", "CRITICAL_ALERT");
    obj.put("message", "Refrigeration failure detected! Cargo at risk.");
} else {
    obj.put("STATUS", "NORMAL");
}
return obj.toString();
