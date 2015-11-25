// expression test

import konoha.hack.Person;

var p = null;
p = new Person("naruto", 17);

// 1. check eval (top level)

int n = (p.age = 18);
assert(p.age == 18);
assert(n == 18);

// 2. check in function

function f(p, age) {
  return p.age = age;
}

assert(f(p, 19) == 19);
