// written by kimio kuramitsu

boolean TestFunc0() {
	int i = 0;
	int c = 0;
	do {
		i = i + 1;
		if(i > 5) {
			break;
		}
		c = c + 1;
	} while(i < 10);
	assert(c == 5);
	return true;
}

boolean TestFunc1() {
	int i = 0;
	int c = 0;
	do {
		i = i + 1;
		if(i % 2 == 0) {
			continue;
		}
		c = c + 1;
	} while(i < 10);
	assert(c == 5);
	return true;
}

boolean TestFunc2() {
	int i = 0;
	do {
		i = i + 1;
	} while(false);
	assert(i == 1);
	return true;
}

assert(TestFunc0());
assert(TestFunc1());
assert(TestFunc2());
