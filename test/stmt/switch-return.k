// try/catch test
// check state

int f(int a) {
  switch(a) {
  case 0:
    return 0;
  case 1:
    return 1;
  case 2:
    return 2;
  default:
    return 3;
  }
  return -1;
}

assert(f(0) == 0);
assert(f(1) == 1);
assert(f(2) == 2);
assert(f(3) == 3);


