// expression test

String s = "";

// 1. check eval (top level)

var h = s.getClass();
assert(h != null);

// 2. check in function

typeof(h) f(String x) {
  return x.getClass();
}

assert(f(s) == h); 

