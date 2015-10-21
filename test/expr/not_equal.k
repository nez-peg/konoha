// expression test

int x = 1;
int y = 2;

// 1. check eval (top level)

assert(x != y);

// 2. check in function

boolean f(int x, int y) {
  return x != y;
}

assert(f(1,2) == true);
