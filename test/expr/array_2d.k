// expression test

// 1. check eval (top level)

int[][] arr = new int[2][2];
arr[0][1] = 20;
assert(arr[0][1] == 20);

// 2. check in function

int[] f() {
  int[][] arr = new int[2][2];
	arr[0][1] = 20;
	return arr[0];
}

int[] b = f();

assert(b[1] == 20);
