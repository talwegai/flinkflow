com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.JsonNode o1 = mapper.readTree(value1);
com.fasterxml.jackson.databind.JsonNode o2 = mapper.readTree(value2);

double sum1 = o1.has("sumTemp") ? o1.get("sumTemp").asDouble() : o1.get("temperature").asDouble();
int count1 = o1.has("count") ? o1.get("count").asInt() : 1;

double sum2 = o2.has("sumTemp") ? o2.get("sumTemp").asDouble() : o2.get("temperature").asDouble();
int count2 = o2.has("count") ? o2.get("count").asInt() : 1;

double totalSum = sum1 + sum2;
int totalCount = count1 + count2;
double avgTemp = Math.round((totalSum / totalCount) * 100.0) / 100.0;

com.fasterxml.jackson.databind.node.ObjectNode result = mapper.createObjectNode();
result.put("truckId", o1.get("truckId").asText());
result.put("sumTemp", totalSum);
result.put("count", totalCount);
result.put("averageTemperature", avgTemp);

return result.toString();
