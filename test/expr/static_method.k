// expression test

import java.lang.Math;

// 1. check eval (top level)

double n = Math.sin(1.0);
assert(n < 1.0);

// 2. check in function

double f() {
  return Math.sin(1.0);
}

assert(f() == n); 

