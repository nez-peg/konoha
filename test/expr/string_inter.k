// expression test

int n = 2;

// 1. check eval (top level)

String s = '''1${n}3''';
assert(s == "123");

// 2. check in function

String f(int n) {
  return '''1${n}3''';
}

assert(f(2) == "123"); 

