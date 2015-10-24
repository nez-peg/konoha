
int state = 0;

int f(int a) {
  int state = a;
  return state;
}

assert(state == 0);
assert(f(1) == 1);
assert(state == 0);

