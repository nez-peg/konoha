// expression test

// 1. check eval (top level)

int n = 1;
int i = 10;
boolean b = (n < 10 || i > 1);
assert(b == true);

// 2. check in function

boolean f(int n, int i) {
  return n < 10 || i > 1;
}

assert(f(1,10) == true);
