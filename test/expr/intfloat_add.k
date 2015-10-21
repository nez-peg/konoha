// expression test

// 1. check eval (top level)

double n = 1 + 1.2;
assert(n == 2.2);

// 2. check in function

double f() {
  return 1 + 1.2;
}

assert(f() == n);

