package konoha.api;

import konoha.Array;
import konoha.ArrayInt;
import konoha.Coercion;
import konoha.Const;

public class ArrayOp {
	@Const
	@Coercion
	public final static <T> T[] to_(Array<T> a) {
		return a.compactArray();
	}

	@Const
	@Coercion
	public final static <T> Array<T> to_(T[] a) {
		return new Array<T>(a);
	}

	@Const
	@Coercion
	public final static int[] to_(ArrayInt a) {
		return a.compactArray();
	}

	@Const
	@Coercion
	public final static ArrayInt to_(int[] a) {
		return new ArrayInt(a);
	}

}
