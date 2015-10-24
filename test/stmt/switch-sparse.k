// try/catch test
// check state

int f(int a) {
  int state = -1;
  switch(a) {
  case 100:
    state = 0;
    break;
  case 1:
    state = 1;
  case 59:
    state = 2;
    break;
  default:
    state = 4;
  }
  return state;
}

assert(f(100) == 0);
assert(f(1) == 2);
assert(f(59) == 2);
assert(f(0) == 4);


