// expression test

var s = null;
s = "abc";

// 1. check eval (top level)

int a = s.size();
int b = s.length();
assert(a == b);

// 2. check in function

int f(x) {
  return x.size();
}

int g(x) {
  return x.length();
}

assert(f(s) == g(s)); 

