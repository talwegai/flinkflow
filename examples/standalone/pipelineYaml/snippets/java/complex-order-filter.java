com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.JsonNode order = mapper.readTree(input);
return "delivered".equals(order.get("status").asText());
