// Split sentence into words and emit each word as a separate record
for (String word : input.split(" ")) {
    out.collect(word);
}
