// expression test

int x = 1;

// 1. check eval (top level)

int n = x += 2;
assert(x == 3);
assert(n == 3);

// 2. check in function

int f(int x, int y) {
  return x += y;
}

assert(f(1,2) == 3);
