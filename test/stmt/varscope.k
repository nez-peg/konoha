

int f(int a) {
  int state = a;
  if(a <= 0) {
    int state = 0;
  }
  else {
    state = a * 2;
  }
  state = state + 1;
  return state;
}

assert(f(1) == 3);
assert(f(0) == 1);
assert(f(-1) == 0);
