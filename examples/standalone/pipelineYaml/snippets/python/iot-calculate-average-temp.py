import json

o1 = json.loads(value1)
o2 = json.loads(value2)

sum1 = o1.get("sumTemp", o1.get("temperature"))
count1 = o1.get("count", 1)
sum2 = o2.get("sumTemp", o2.get("temperature"))
count2 = o2.get("count", 1)

total_sum = sum1 + sum2
total_count = count1 + count2

return json.dumps({
    "truckId": o1["truckId"],
    "sumTemp": total_sum,
    "count": total_count,
    "averageTemperature": round(total_sum / total_count, 2)
})
