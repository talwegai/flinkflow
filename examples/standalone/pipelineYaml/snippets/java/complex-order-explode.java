com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.JsonNode order = mapper.readTree(input);
for (com.fasterxml.jackson.databind.JsonNode item : order.get("items")) {
    com.fasterxml.jackson.databind.node.ObjectNode record = mapper.createObjectNode();
    record.put("order_id", order.get("id").asText());
    record.put("item_name", item.asText());
    record.put("total_amount", order.get("total_amount").asDouble());
    record.put("processed_at", order.get("processed_at").asText());
    out.collect(record.toString());
}
