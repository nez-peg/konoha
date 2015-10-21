// expression test

// 1. check eval (top level)

int n = true ? 1 : 0;
int m = false ? 1 : 0;
assert(n == 1);
assert(m == 0);

// 2. check in function

int f() {
  return true ? 1 : 0;
}

assert(f() == 1);

int g() {
  return false ? 1 : 0;
}

assert(g() == 0);


