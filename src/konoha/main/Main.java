package konoha.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import konoha.message.Message;
import konoha.script.EmptyResult;
import konoha.script.ScriptContext;
import konoha.script.ScriptRuntimeException;
import nez.main.CommandContext;

public class Main {
	public final static void main(String[] args) {
		try {
			CommandContext c = new CommandContext();
			c.parseCommandOption(args, false/* nezCommand */);
			exec(c);
		} catch (IOException e) {
			ConsoleUtils.println(e);
			System.exit(1);
		}
	}

	public static void exec(CommandContext config) throws IOException {
		if (config.isUnspecifiedGrammarFilePath()) {
			config.setGrammarFilePath("konoha.nez");
		}
		ScriptContext sc = new ScriptContext(config.newParser());
		if (config.hasInput()) {
			while (config.hasInput()) {
				sc.eval(config.nextInput());
			}
		} else {
			show(config);
			shell(sc);
		}
	}

	public final static String ProgName = "Konoha";
	public final static String CodeName = "yokohama";
	public final static int MajorVersion = 4;
	public final static int MinerVersion = 0;
	public final static int PatchLevel = Revision.REV;
	public final static String Version = "" + MajorVersion + "." + MinerVersion + "-" + PatchLevel;
	public final static String Copyright = "Copyright (c) 2008-2015, Konoha project authors";
	public final static String License = "BSD-License Open Source";

	private static void show(CommandContext config) {
		ConsoleUtils.bold();
		ConsoleUtils.println("Konoha " + Version + " U(" + config.newGrammar().getDesc() + ") on Nez " + nez.main.Command.Version);
		ConsoleUtils.end();
		ConsoleUtils.println(Copyright);
		ConsoleUtils.println("Copyright (c) 2015, Kimio Kuramitsu, Yokohama National University");
		ConsoleUtils.begin(37);
		ConsoleUtils.println(Message.Hint);
		ConsoleUtils.end();
	}

	public static void shell(ScriptContext sc) throws IOException {
		sc.setShellMode(true);
		ConsoleReader console = new ConsoleReader();
		console.setHistoryEnabled(true);
		console.setExpandEvents(false);
		int linenum = 1;
		String command = null;
		while ((command = readLine(console)) != null) {
			if (command.trim().equals("")) {
				continue;
			}
			if (hasUTF8(command)) {
				ConsoleUtils.begin(31);
				ConsoleUtils.println("(<stdio>:" + linenum + ") " + Message.DetectedUTF8);
				ConsoleUtils.end();
				command = filterUTF8(command);
			}

			try {
				ConsoleUtils.begin(32);
				Object result = sc.eval2("<stdio>", linenum, command);
				ConsoleUtils.end();
				if (!(result instanceof EmptyResult)) {
					ConsoleUtils.begin(37);
					ConsoleUtils.println("<<<");
					ConsoleUtils.end();
					ConsoleUtils.bold();
					ConsoleUtils.println(result);
					ConsoleUtils.end();
				}
			} catch (ScriptRuntimeException e) {
				ConsoleUtils.begin(31);
				ConsoleUtils.println(e);
				e.printStackTrace();
				ConsoleUtils.end();
			} catch (RuntimeException e) {
				ConsoleUtils.begin(31);
				ConsoleUtils.println(e);
				e.printStackTrace();
				ConsoleUtils.end();
			}
			linenum += (command.split("\n").length);
		}
	}

	public final static String KonohaVersion = "4.0";

	private static String readLine(ConsoleReader console) throws IOException {
		ConsoleUtils.begin(31);
		ConsoleUtils.println(">>>");
		ConsoleUtils.end();
		List<Completer> completors = new LinkedList<Completer>();

		// ConsoleReader console = new ConsoleReader();
		// completors.add(new AnsiStringsCompleter("\u001B[1mfoo\u001B[0m",
		// "bar", "\u001B[32mbaz\u001B[0m"));
		// CandidateListCompletionHandler handler = new
		// CandidateListCompletionHandler();
		// handler.setStripAnsi(true);
		// console.setCompletionHandler(handler);
		// for (Completer c : completors) {
		// console.addCompleter(c);
		// }
		// History h = console.getHistory();
		// ("hoge\rhoge");
		StringBuilder sb = new StringBuilder();
		while (true) {
			String line = console.readLine();
			if (line == null) {
				return null;
			}
			if (line.equals("")) {
				return sb.toString();
			}
			sb.append(line);
			sb.append("\n");
		}
		// h = console.getHistory();
	}

	private static boolean hasUTF8(String command) {
		boolean skip = false;
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"') {
				skip = !skip;
				continue;
			}
			if (c < 128 || skip) {
				continue;
			}
			return true;
		}
		return false;
	}

	static HashMap<Character, Character> charMap = null;

	static void initCharMap() {
		if (charMap == null) {
			charMap = new HashMap<>();
			charMap.put('　', ' ');
			charMap.put('（', '(');
			charMap.put('）', ')');
			charMap.put('［', '[');
			charMap.put('］', ']');
			charMap.put('｛', '{');
			charMap.put('｝', '}');
			charMap.put('”', '"');
			charMap.put('’', '\'');
			charMap.put('＜', '<');
			charMap.put('＞', '>');
			charMap.put('＋', '+');
			charMap.put('ー', '-');
			charMap.put('＊', '*');
			charMap.put('／', '/');
			charMap.put('✕', '*');
			charMap.put('÷', '/');
			charMap.put('＝', '=');
			charMap.put('％', '%');
			charMap.put('？', '?');
			charMap.put(':', ':');
			charMap.put('＆', '&');
			charMap.put('｜', '|');
			charMap.put('！', '!');
			charMap.put('、', ',');
			charMap.put('；', ';');
			charMap.put('。', '.');
			for (char c = 'A'; c <= 'Z'; c++) {
				charMap.put((char) ('Ａ' + (c - 'A')), c);
			}
			for (char c = 'a'; c <= 'z'; c++) {
				charMap.put((char) ('ａ' + (c - 'a')), c);
			}
			for (char c = '0'; c <= '9'; c++) {
				charMap.put((char) ('０' + (c - '0')), c);
			}
			for (char c = '1'; c <= '9'; c++) {
				charMap.put((char) ('一' + (c - '0')), c);
			}
		}
	}

	private static String filterUTF8(String command) {
		initCharMap();
		StringBuilder sb = new StringBuilder(command.length());
		boolean skip = false;
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c < 128 || skip) {
				if (c == '"') {
					skip = !skip;
				}
				sb.append(c);
				continue;
			}
			Character mapped = charMap.get(c);
			if (mapped != null) {
				sb.append(mapped);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/****
	 * public static void usage() { System.out.println("Usage: java " +
	 * Example.class.getName() +
	 * " [none/simple/files/dictionary [trigger mask]]");
	 * System.out.println("  none - no completors");
	 * System.out.println("  simple - a simple completor that comples " +
	 * "\"foo\", \"bar\", and \"baz\"");
	 * System.out.println("  files - a completor that comples " + "file names");
	 * System.out.println("  classes - a completor that comples " +
	 * "java class names"); System.out.println(
	 * "  trigger - a special word which causes it to assume " +
	 * "the next line is a password");
	 * System.out.println("  mask - is the character to print in place of " +
	 * "the actual password character");
	 * System.out.println("  color - colored prompt and feedback");
	 * System.out.println("\n  E.g - java Example simple su '*'\n" +
	 * "will use the simple compleator with 'su' triggering\n" +
	 * "the use of '*' as a password mask."); }
	 * 
	 * public static void main2(String[] args) throws IOException { try {
	 * Character mask = null; String trigger = null; boolean color = false;
	 * 
	 * ConsoleReader reader = new ConsoleReader();
	 * 
	 * reader.setPrompt("prompt> ");
	 * 
	 * if ((args == null) || (args.length == 0)) { usage();
	 * 
	 * return; }
	 * 
	 * List<Completer> completors = new LinkedList<Completer>();
	 * 
	 * if (args.length > 0) { if (args[0].equals("none")) { } else if
	 * (args[0].equals("files")) { completors.add(new FileNameCompleter()); }
	 * else if (args[0].equals("simple")) { completors.add(new
	 * StringsCompleter("foo", "bar", "baz")); } else if
	 * (args[0].equals("color")) { color = true;
	 * reader.setPrompt("\u001B[42mfoo\u001B[0m@bar\u001B[32m@baz\u001B[0m> ");
	 * completors.add(new AnsiStringsCompleter("\u001B[1mfoo\u001B[0m", "bar",
	 * "\u001B[32mbaz\u001B[0m")); CandidateListCompletionHandler handler = new
	 * CandidateListCompletionHandler(); handler.setStripAnsi(true);
	 * reader.setCompletionHandler(handler); } else { usage();
	 * 
	 * return; } }
	 * 
	 * if (args.length == 3) { mask = args[2].charAt(0); trigger = args[1]; }
	 * 
	 * 
	 * String line; PrintWriter out = new PrintWriter(reader.getOutput());
	 * 
	 * while ((line = reader.readLine()) != null) { if (color) {
	 * out.println("\u001B[33m======>\u001B[0m\"" + line + "\"");
	 * 
	 * } else { out.println("======>\"" + line + "\""); } out.flush();
	 * 
	 * // If we input the special word then we will mask // the next line. if
	 * ((trigger != null) && (line.compareTo(trigger) == 0)) { line =
	 * reader.readLine("password> ", mask); } if (line.equalsIgnoreCase("quit")
	 * || line.equalsIgnoreCase("exit")) { break; } if
	 * (line.equalsIgnoreCase("cls")) { reader.clearScreen(); } } } catch
	 * (Throwable t) { t.printStackTrace(); } }
	 ***/
}
