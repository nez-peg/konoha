// int

int x = 7;
int y = 3;

assert(x + y == 10);
assert(x - y == 4);
assert(x * y == 21);
assert(x / y == 2);
assert(x % y == 1);

assert(!(x == y));
assert(x != y);
assert(!(x < y));
assert(x > y);
assert(!(x <= y));
assert(x >= y);

void f(int x, int y) {
 assert(x + y == 10);
 assert(x - y == 4);
 assert(x * y == 21);
 assert(x / y == 2);
 assert(x % y == 1);
 assert(!(x == y));
 assert(x != y);
 assert(!(x < y));
 assert(x > y);
 assert(!(x <= y));
 assert(x >= y);
}

f(7, 3);

int a = 0b0001;
int b = 0b0010;

assert(a & b == 0);
assert(a | b == 0b0011);
assert(~0 == -1);

println("int ok");

