// expression test

// 1. check eval (top level)

int[] n = {1,2};
assert(n[0] < n[1]);

// 2. check in function

int[] f() {
  int[] a = {1,2};
  return a;
}

int[] b = f();

assert(b[0] < b[1]);
