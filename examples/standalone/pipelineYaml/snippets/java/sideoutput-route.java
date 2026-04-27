if (input.startsWith("ERROR:")) {
    ctx.output(input);
} else {
    out.collect(input);
}
