// expression test

// 1. check eval (top level)

assert(1 < 1.1);

// 2. check in function

boolean f() {
  return 1 < 1.1;
}

assert(f());

