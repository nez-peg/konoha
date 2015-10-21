// expression test

// 1. check eval (top level)

int n = 5 ^ 15;
assert(n == 10);

// 2. check in function

int f(int n, int i) {
  return n ^ i;
}

assert(f(5,15) == 10);
