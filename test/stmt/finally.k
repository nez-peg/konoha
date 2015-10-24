// try/finally test
// check state

int state = 0;

int h(int a, int b) {
  return a/b;
}

void g(int a, int b) {
  state = 0;
  try {
    state = 1;
    h(a, b);
    state = 2;
  }
  finally {
    state = 3;
  }
}

int f(int a, int b) {
  try {
    g(a,b);
    return 1; 
  }
  catch(RuntimeException e) {
    return 2;
  }
  return 0;
}

assert(f(1,1) == 1);
println("state="+state);
assert(state == 3);
assert(f(1,0) == 2);
println("state="+state);
assert(state == 3);


