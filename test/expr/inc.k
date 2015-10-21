// expression test

// 1. check eval (top level)

int n = 0;
assert(n++ == 0);

// 2. check in function

int f(int n) {
  return n++;
}

assert(f(0) == 0); 

