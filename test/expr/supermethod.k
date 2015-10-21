// expression test

String s = "";

// 1. check eval (top level)

int h = s.hashCode();
assert(h != 0);

// 2. check in function

int f(String x) {
  return x.hashCode();
}

assert(f(s) != 0); 

