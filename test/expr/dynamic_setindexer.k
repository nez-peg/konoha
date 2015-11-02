// expression test

var a = null;
var b = null;
a = [1,2];
b = ["a", "b"];

// 1. check eval (top level)

int n = (a[0] = 3);
String s = (b[0] = "x");
assert(a[0] == 3);
assert(b[0] == "x");
assert(n == 3);
assert(s == "x");

// 2. check in function

function f( x) {
  return x[0] = 4;
}

function g( y) {
	return y[0] = "y";
}

assert(f(a) == 4);
assert(g(b) == "y");
assert(a[0] == 4);
assert(b[0] == "y");


