// try/catch test
// check state

int f(int a, int b) {
  int state = 0;
  try {
	state = 1;
        state = a/b;
        state = 2;  
  }
  catch(RuntimeException e) {
    state = 3;
  }
  finally {
    state = state + 1;
  }
  return state;
}

assert(f(1,1) == 3);
assert(f(1,0) == 4);

