// expression test

// 1. check eval (top level)

int[] a = new int[10];
String[] s = new String[10];
assert(a.size() == 10);
assert(s.size() == 10);

// 2. check in function

int[] f() {
  return new int[10];
}

String[] g() {
  return new String[10];
}

assert(f().size() == 10); 
assert(g().size() == 10);

