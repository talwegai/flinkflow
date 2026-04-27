String device = value1.split(",")[0];
int val1 = Integer.parseInt(value1.split(",")[1]);
int val2 = Integer.parseInt(value2.split(",")[1]);
return device + "," + (val1 + val2);
