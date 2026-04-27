def v1 = headers.get("value1")
def v2 = headers.get("value2")
def device = v1.split(",")[0]
return device + "," + (v1.split(",")[1].toInteger() + v2.split(",")[1].toInteger())
