// expression test

// 1. check eval (top level)

String s = "";
assert(s != null);

// 2. check in function

String f() {
  return "";
}

assert(f() != null); 

