// expression test

int n = 2;

// 1. check eval (top level)

String s = null;
assert(s == null);

// 2. check in function

String f() {
  return null;
}

assert(f() == null); 

