//import konoha.hack.AST;
//import konoha.hack.ByteCode;

function test(){
	var f = (l, n)->{
		int offset = 100;
		return offset - n - l;
	};
	return f;
}

function run(){
	var f = test();
	return(f(10,20));
}

var lam = (int n, int m, l)->{
		int offset = 100;
		return offset + n + m + l;
	};

assert(run() == 70);
assert(lam(20,30,40) == 190);
