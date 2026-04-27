def left = body.toString()
def right = headers.get("right").toString()
return "Order: ${left.split(',')[2]} | Customer: ${right.split(',')[1]}"
