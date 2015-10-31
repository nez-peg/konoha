package konoha.asm;

import konoha.script.TypeSystem;
import nez.util.UList;

public class ConstPools {
	static TypeSystem[] typeSystems = new TypeSystem[0];
	static UList<Object[]> constList = new UList<Object[]>(new Object[4][]);

	public static int typeSystemId(TypeSystem ts) {
		for (int i = 0; i < typeSystems.length; i++) {
			if (typeSystems[i] == ts) {
				return i;
			}
		}
		return -1;
	}

	public static int registTypeSystem(TypeSystem ts) {
		int id = typeSystemId(ts);
		if (id == -1) {
			TypeSystem[] newarray = new TypeSystem[typeSystems.length + 1];
			System.arraycopy(typeSystems, 0, newarray, 0, typeSystems.length);
			newarray[(id = typeSystems.length)] = ts;
			typeSystems = newarray;
		}
		return id;
	}

	public final static TypeSystem typeSystem(int id) {
		return typeSystems[id];
	}

	static int registConstPools(Object[] pools) {
		int id = constList.size();
		constList.add(pools);
		return id;
	}

	public final static Object get(int id, int id2) {
		return constList.ArrayValues[id][id2];
	}

}
