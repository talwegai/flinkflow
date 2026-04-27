order_details = left.split(",")[2]
customer_name = right.split(",")[1]
return f"Order: {order_details} | Customer: {customer_name}"
