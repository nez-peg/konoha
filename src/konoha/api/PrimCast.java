package konoha.api;

public class PrimCast {
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
