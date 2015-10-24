// for test
// check state

int f(int a) {
  int state = 0;
  for(int i = 0; i < a; i++) {
    int b = 1;
    state += b;
  }
  return state;
}

assert(f(0) == 0);
assert(f(1) == 1);
assert(f(2) == 2);
assert(f(10000) == 10000);


