// written by kimio kuramitsu

boolean TestIntFunc() {
	int a = 3;
	a += 2;
	assert(a == 5);
	a -= 3;
	assert(a == 2);
	a *= 4;
	assert(a == 8);
	a /= 3;
	assert(a == 2);
	return true;
}

assert(TestIntFunc());
