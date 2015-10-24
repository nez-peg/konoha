// for test
// check state

int f(int a) {
  int state = 0;
  int i = 0;
  while(i < a) {
    if(i % 2 == 1) {
      i++;
      continue;
    }
    state += 1;
    i++;
  }
  return state;
}

assert(f(0) == 0);
assert(f(1) == 1);
assert(f(2) == 1);
assert(f(3) == 2);
assert(f(4) == 2);
assert(f(10000) == 5000);


