// for test
// check state

int f(int a) {
  int state = 0;
  for(int i = 0; i < a; i++) {
    if(i % 2 == 1) {
      continue;
    }
    state += 1;
  }
  return state;
}

assert(f(0) == 0);
assert(f(1) == 1);
assert(f(2) == 1);
assert(f(3) == 2);
assert(f(4) == 2);
assert(f(10000) == 5000);


