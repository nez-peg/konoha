// try/catch test
// check state

int f(int a) {
  int state = -1;
  switch(a) {
  case 0:
    state = 0;
    break;
  case 1:
    state = 1;
  case 2:
    state = 2;
    break;
  default:
    state = 4;
  }
  return state;
}

assert(f(0) == 0);
assert(f(1) == 2);
assert(f(2) == 2);
assert(f(4) == 4);


