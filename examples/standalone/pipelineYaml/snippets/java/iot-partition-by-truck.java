com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.JsonNode obj = mapper.readTree(input);
return obj.get("truckId").asText();
