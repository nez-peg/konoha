package konoha;

public class StaticOperator {

	/* boolean */
	public final static boolean opNot(boolean a) {
		return !a;
	}

	public final static boolean opEquals(boolean a, boolean b) {
		return a == b;
	}

	public final static boolean opNotEquals(boolean a, boolean b) {
		return a != b;
	}

	/* int */

	/* double */
	public final static double opPlus(double a) {
		return +a;
	}

	public final static double opMinus(double a) {
		return -a;
	}

	public final static double opAdd(double a, double b) {
		return a + b;
	}

	public final static double opSub(double a, double b) {
		return a - b;
	}

	public final static double opMul(double a, double b) {
		return a * b;
	}

	public final static double opDiv(double a, double b) {
		return a / b;
	}

	public final static double opMod(double a, double b) {
		return a % b;
	}

	public final static boolean opEquals(double a, double b) {
		return a == b;
	}

	public final static boolean opNotEquals(double a, double b) {
		return a != b;
	}

	public final static boolean opLessThan(double a, double b) {
		return a < b;
	}

	public final static boolean opGreaterThan(double a, double b) {
		return a > b;
	}

	public final static boolean opLessThanEquals(double a, double b) {
		return a <= b;
	}

	public final static boolean opGreaterThanEquals(double a, double b) {
		return a >= b;
	}

	/* long */

	public final static long opPlus(long a) {
		return +a;
	}

	public final static long opMinus(long a) {
		return -a;
	}

	public final static long opAdd(long a, long b) {
		return a + b;
	}

	public final static long opSub(long a, long b) {
		return a - b;
	}

	public final static long opMul(long a, long b) {
		return a * b;
	}

	public final static long opDiv(long a, long b) {
		return a / b;
	}

	public final static long opMod(long a, long b) {
		return a % b;
	}

	public final static boolean opEquals(long a, long b) {
		return a == b;
	}

	public final static boolean opNotEquals(long a, long b) {
		return a != b;
	}

	public final static boolean opLessThan(long a, long b) {
		return a < b;
	}

	public final static boolean opGreaterThan(long a, long b) {
		return a > b;
	}

	public final static boolean opLessThanEquals(long a, long b) {
		return a <= b;
	}

	public final static boolean opGreaterThanEquals(long a, long b) {
		return a >= b;
	}

	public final static long opLeftShift(long a, long b) {
		return a << b;
	}

	public final static long opRightShift(long a, long b) {
		return a >> b;
	}

	public final static long opLogicalRightShift(long a, long b) {
		return a >>> b;
	}

	public final static long opBitwiseAnd(long a, long b) {
		return a & b;
	}

	public final static long opBitwiseOr(long a, long b) {
		return a | b;
	}

	public final static long opBitwiseXor(long a, long b) {
		return a ^ b;
	}

	public final static long opCompl(long a) {
		return ~a;
	}

	/* double */

	public final static double to_double(Double a) {
		return a.doubleValue();
	}

	public final static double to_double(float a) {
		return a;
	}

	public final static double to_double(Float a) {
		return a.doubleValue();
	}

	public final static double to_double(long a) {
		return a;
	}

	public final static double to_double(Long a) {
		return a.doubleValue();
	}

	public final static double to_double(int a) {
		return a;
	}

	public final static double to_double(Integer a) {
		return a.doubleValue();
	}

	/* long */

	public final static long to_long(Long a) {
		return a.longValue();
	}

	public final static long to_long(double a) {
		return (long) a;
	}

	public final static long to_long(Double a) {
		return a.longValue();
	}

	public final static long to_long(float a) {
		return (long) a;
	}

	public final static long to_long(Float a) {
		return a.longValue();
	}

	public final static long to_long(int a) {
		return a;
	}

	public final static long to_long(Integer a) {
		return a.longValue();
	}

	public final static long to_long(short a) {
		return a;
	}

	public final static long to_long(Short a) {
		return a.longValue();
	}

	public final static long to_long(byte a) {
		return a & 0xff;
	}

	public final static long to_long(Byte a) {
		return a.longValue();
	}

}
