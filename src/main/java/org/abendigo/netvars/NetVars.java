package org.abendigo.netvars;

import org.abendigo.netvars.impl.ClientClass;
import org.abendigo.netvars.impl.RecvProp;
import org.abendigo.netvars.impl.RecvTable;
import org.abendigo.process.Module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.abendigo.misc.PatternScanner.READ;
import static org.abendigo.misc.PatternScanner.getAddressForPattern;


/**
 * Created by Jonathan on 11/16/2015.
 */
public final class NetVars {

	private static final List<NetVar> netVars = new ArrayList<>();

	public static void load(Module clientModule) {
		int firstclass = getAddressForPattern(clientModule, 0, 0, 0, "DT_TEWorldDecal");
		firstclass = getAddressForPattern(clientModule, 0x2B, 0, READ, firstclass);

		for (ClientClass clientClass = new ClientClass(firstclass); clientClass.readable(); clientClass = new ClientClass(clientClass.next())) {
			RecvTable table = new RecvTable(clientClass.table());
			if (!table.readable()) {
				continue;
			}
			scanTable(table, 0, table.tableName());
		}
	}

	public static void dump() {
		List<String> text = new ArrayList<>();
		netVars.forEach(n -> text.add(n.toString()));
		try {
			Files.write(Paths.get("NetVars.txt"), text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void scanTable(RecvTable table, int offset, String name) {
		int count = table.propCount();
		for (int i = 0; i < count; i++) {
			RecvProp prop = new RecvProp(table.propForId(i), offset);

			if (Character.isDigit(prop.name().charAt(0))) {
				continue;
			}

			boolean isBaseClass = prop.name().contains("baseclass");
			if (!isBaseClass) {
				netVars.add(new NetVar(name, prop.name(), prop.offset()));
			}

			int child = prop.table();
			if (child == 0) {
				continue;
			}
			scanTable(new RecvTable(child), prop.offset(), name);
		}
	}

	public static int byName(String className, String varname) {
		for (NetVar var : netVars) {
			if (var.className.equals(className) && var.varName.equals(varname)) {
				return var.offset;
			}
		}
		throw new RuntimeException("NetVar [" + className + ", " + varname + "] not found!");
	}

	private static class NetVar {

		private final String className;
		private final String varName;
		private final int offset;

		private NetVar(String className, String varName, int offset) {
			this.className = className;
			this.varName = varName;
			this.offset = offset;
		}

		@Override
		public String toString() {
			return className + " " + varName + " = " + "0x" + Integer.toHexString(offset).toUpperCase();
		}

	}

}
