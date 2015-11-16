//import konoha.hack.AST;
//import konoha.hack.ByteCode;

function test(){
	int i = 0;
	var a = ()->{
		return i++;
	};
	return a;
}

var f1 = test();
assert(f1() == 0);
assert(f1() == 1);
assert(f1() == 2);
assert(f1() == 3);

var f2 = test();
assert(f2() == 0);
assert(f2() == 1);
assert(f2() == 2);
assert(f2() == 3);

int i = 0;

var inc = ()->{
	var a = 10;
	return a + i++;
};

assert(inc() == 10);
assert(inc() == 11);
assert(inc() == 12);
assert(inc() == 13);
