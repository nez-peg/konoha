// expression test

import java.lang.Math;

// 1. check eval (top level)

double n = Math.PI;
assert(n != 0.0);

// 2. check in function

double f() {
  return Math.PI;
}

assert(n == f()); 

