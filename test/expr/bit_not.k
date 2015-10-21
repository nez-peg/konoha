// expression test

int b = 1;

// 1. check eval (top level)

b = ~b + 1 ;
assert(b == -1);

// 2. check in function

int f(int b) {
  return ~b + 1;
}

assert(f(1) == -1);
