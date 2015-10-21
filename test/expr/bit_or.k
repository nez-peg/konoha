// expression test

// 1. check eval (top level)

int n = 5 | 10;
assert(n == 15);

// 2. check in function

int f(int n, int i) {
  return n | i;
}

assert(f(5,10) == 15);
