package konoha.asm;

import org.objectweb.asm.Label;

/**
 * Left : Index, Right: Key
 * **/
public class SwitchCase implements Comparable<SwitchCase> {

	private int index;
	private int key;
	private Object value;
	private Label evalLabel;
	private Label checklabel; // for StringSwitch

	public SwitchCase(int index, int key) {
		this.index = index;
		this.key = key;
		this.value = null;
	}

	public SwitchCase(int index, int key, Object value) {
		this.index = index;
		this.key = key;
		this.value = value;
	}

	public int getIndex() {
		return index;
	}

	public int getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public Label getEvalLabel() {
		return evalLabel;
	}

	public void setEvalLabel(Label evalLabel) {
		this.evalLabel = evalLabel;
	}

	public Label getChecklabel() {
		return checklabel;
	}

	public void setChecklabel(Label checklabel) {
		this.checklabel = checklabel;
	}

	@Override
	public int compareTo(SwitchCase target) {
		int tkey = target.getKey();
		int res = ((Integer) this.key).compareTo(tkey);
		if (res == 0) {
			return ((Integer) this.index).compareTo(target.getIndex());
		}
		return res;
	}

	@Override
	public String toString() {
		String out = "";
		out += "Key: " + this.key + "\n";
		out += "Index: " + this.index + "\n";
		out += "Value: " + this.value + "\n";
		return out;
	}

}
