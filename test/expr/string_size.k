// expression test

String s = "abc";

// 1. check eval (top level)

int a = s.size();
int b = s.length();
assert(a == b);

// 2. check in function

int f(String x) {
  return x.size();
}

int g(String x) {
  return x.length();
}

assert(f(s) == g(s)); 

