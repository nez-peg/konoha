// expression test

import java.lang.Math;

// 1. check eval (top level)

s = "hoge";
assert(s.length() == 4);

// 2. check in function

int f(s) {
  return s.length();
}

assert(f("hoge") == 4); 

