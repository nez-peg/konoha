// expression test

int b = 16;

// 1. check eval (top level)

b << 2;
assert(b == 16);

// 2. check in function

int f(int b) {
  return b << 2;
}

assert(f(16) == 64);
