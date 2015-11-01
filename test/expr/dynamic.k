// expression test

// 1. check eval (top level)

var a = null;
a = 1;
assert(a + 1 == 2);
a = 0.5;
assert(a + 1 == 1.5);
a = "1";
assert(a + 1 == "11");

// 2. check in function

function f(x) {
  return x+1;
}

assert(f(1) == 2);
assert(f(0.5) == 1.5);
assert(f("1") == "11");

