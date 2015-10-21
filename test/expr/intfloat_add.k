// expression test

// 1. check eval (top level)

double n = 1 + 1.2;
assert(typeof(n) == typeof(1+1.2));

// 2. check in function

double f() {
  return 1 + 1.2;
}

assert(typeof(f()) == typeof(n));

