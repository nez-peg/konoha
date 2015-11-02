// expression test

var a = null;
var b = null;
a = [1,2];
b = ["a", "b"];

// 1. check eval (top level)

int n = a[0];
String s = b[0];
assert(n == 1);
assert(s == "a");

// 2. check in function

function f(x) {
  return x[0];
}

function g(y) {
	return y[0];
}

assert(f(a) == 1);
assert(g(b) == "a");

