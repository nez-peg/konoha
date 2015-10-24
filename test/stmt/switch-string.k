// try/catch test
// check state

int f(String a) {
  int state = -1;
  switch(a) {
  case "apple":
    state = 0;
    break;
  case "banana":
    state = 1;
  case "orange":
    state = 2;
    break;
  default:
    state = 4;
  }
  return state;
}

assert(f("apple") == 0);
assert(f("banana") == 2);
assert(f("orange") == 2);
assert(f("mikan") == 4);


