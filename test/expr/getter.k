// expression test

import konoha.hack.Person;

Person p = new Person("naruto", 17);

// 1. check eval (top level)

int n = p.age;
assert(n == 17);

// 2. check in function

int f(Person p) {
  return p.age;
}

assert(f(p) == 17); 

int g() {
	return p.age;
}

assert(g() == 17);


