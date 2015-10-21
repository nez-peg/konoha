// expression test

import konoha.hack.Person;

// 1. check eval (top level)

Person p = new Person("naruto", 17);
assert(p != null);

// 2. check in function

Person f() {
  return new Person("naruto", 17);
}

assert(f() != null); 

