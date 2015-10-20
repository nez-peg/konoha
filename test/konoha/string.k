

function TestString(abc: String) {
println("#abc: %s",  abc);
println("#abc.length(): %s",  abc.length());
println("#abc[1]: %s",  abc[1]);

println("#abc.substring(1): %s",  abc.substring(1));
println("#abc.substring(0,1): %s",  abc.substring(0,1));
println("#abc.indexOf(\"bc\"): %s",  abc.indexOf("bc"));
println("#abc.indexOf(\"z\"): %s",  abc.indexOf("z")); 
println("#abc == \"abc\": %s",  (abc == "abc"));
println("#abc == \"ABC\": %s",  (abc == "ABC"));
println("#abc.startsWith(\"ab\"): %s",  abc.startsWith("ab"));
println("#abc.startsWith(\"abcd\"): %s",  abc.startsWith("abcd"));
println("#abc.endsWith(\"bc\"): %s",  abc.endsWith("bc"));
println("#abc.endsWith(\"dabc\"): %s",  abc.endsWith("dabc"));

println("#abc+abc: %s",  (abc+abc));
//println("#abc+null: %s",  (abc+null));
//println("#null+abc: %s",  (null+abc));
println("#abc+1: %s",  (abc+1));
println("#1+abc: %s",  (1+abc));

assert(abc.length() == 3);
assert(abc[1] == "b");
assert(abc.substring(1) == "bc");
assert(abc.substring(0,1) == "a");
assert(abc.indexOf("bc") == 1);
assert(abc.indexOf("z") == -1);
assert(abc == "abc");
assert((abc == "ABC") == false);
assert(abc.startsWith("ab"));
assert(!abc.startsWith("abcd"));
assert(abc.endsWith("bc"));
assert(!abc.endsWith("dabc"));

assert(abc+abc == "abcabc");
//assert(abc+null == "abcnull");
//assert(null+abc == "nullabc");
assert(abc+1 == "abc1");
assert(1+abc == "1abc");
}

TestString("abc");
