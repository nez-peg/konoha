
int a,b=1;
assert(a == 0);
assert(b == 1);
String s,t="a";
assert(s == null);
assert(t == "a");

void f() {
  int a,b=1;
  String s,t="a";
  assert(a==0);
  assert(b==1);
  assert(s== null);
  assert(t=="a");
}

f();

