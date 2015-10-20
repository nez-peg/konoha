// written by kimio kuramitsu

function ReturnBoolean() : boolean {
	assert(true);
}

function ReturnInt() : int {
	assert(true);
}

function ReturnString() : String {
	assert(true);
}

function ReturnVoid() {
	assert(true);
}

function main() {
	ReturnVoid();
	println("#return default boolean: %s ", ReturnBoolean());
	println("#return default int: %s", ReturnInt());
	println("#return default String: %s", ReturnString()); 
	assert(!ReturnBoolean());
	assert(ReturnInt() == 0);
	ReturnVoid();
}

main()

