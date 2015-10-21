// expression test

int num = 3394128;

// 1. check eval (top level)

int n = num;
assert(n == num);

// 2. check in function

int f() {
  return n;
}

assert(f() == num); 

