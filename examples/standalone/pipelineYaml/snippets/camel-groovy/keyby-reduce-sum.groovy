def v1 = headers.get("value1")
def v2 = headers.get("value2")
def fruit = v1.split(",")[0]
return fruit + "," + (v1.split(",")[1].toInteger() + v2.split(",")[1].toInteger())
