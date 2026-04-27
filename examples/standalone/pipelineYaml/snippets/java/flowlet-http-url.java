String customerId = input.split(",")[1].trim();
return "https://internal-customer-api.svc.cluster.local/customers/" + customerId;
