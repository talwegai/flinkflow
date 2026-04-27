customer_id = input.split(",")[1].strip()
return f"https://internal-customer-api.svc.cluster.local/customers/{customer_id}"
