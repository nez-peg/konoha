// expression test
import konoha.hack.AST;
// 1. check eval (top level)

int[][] arr = {[1,2],[3,4]};
arr[0][0] = 5;
//assert(arr[0][0] > arr[1][1]);

// 2. check in function

int[][] f() {
	int[][] arr = {[1,2],[3,4]};
	arr[0][0] = 5;
	return arr;
}

int[][] b = f();

assert(b[0][0] > b[1][1]);
