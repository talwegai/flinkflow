String fruit = value1.split(",")[0];
int count1 = Integer.parseInt(value1.split(",")[1]);
int count2 = Integer.parseInt(value2.split(",")[1]);
return fruit + "," + (count1 + count2);
