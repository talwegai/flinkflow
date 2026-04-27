String[] parts = input.split("-");
long index = Long.parseLong(parts[parts.length - 1]);
String[] trucks = {"TRUCK_01", "TRUCK_02", "TRUCK_03"};
String truckId = trucks[(int)(index % 3)];
double temp = Math.round((-18.0 + (Math.random() * 58.0)) * 100.0) / 100.0;
return "{\"truckId\":\"" + truckId + "\",\"temperature\":" + temp + "}";
