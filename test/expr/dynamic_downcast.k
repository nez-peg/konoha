// expression test

// 1. check eval (top level)

var a = null;
a = 1;
int n = a;
assert(n == 1);
a = "1";
String s = a;
assert(s == "1");

// 2. check in function

int f(x) {
  return x;
}

String g(x) {
  return x;
}

assert(f(1) == 1);
assert(g("1") == "1");

