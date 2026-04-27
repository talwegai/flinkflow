com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
com.fasterxml.jackson.databind.node.ObjectNode order = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(input);

double amount = order.get("amount").asDouble();
double tax = Math.round(amount * 0.07 * 100.0) / 100.0;
double total = Math.round((amount + tax) * 100.0) / 100.0;

order.put("tax_amount", tax);
order.put("total_amount", total);
order.put("processed_at", java.time.Instant.now().toString());
order.put("engine", "Janino-Java");

return order.toString();
