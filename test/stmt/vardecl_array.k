
int a[10];
assert(a.size() == 10);
String s[10];
assert(s.size() == 10);

void f() {
  int a[10];
  String s[10];
  assert(a.size() == 10);
  assert(s.size() == 10);
}

f();

