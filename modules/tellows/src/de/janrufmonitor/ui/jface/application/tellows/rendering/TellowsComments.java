package de.janrufmonitor.ui.jface.application.tellows.rendering;

import de.janrufmonitor.framework.IAttributeMap;
import de.janrufmonitor.framework.ICall;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.ui.jface.application.rendering.AbstractTableCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IEditorCellRenderer;
import de.janrufmonitor.ui.jface.application.rendering.IJournalCellRenderer;

public class TellowsComments extends AbstractTableCellRenderer implements IJournalCellRenderer, IEditorCellRenderer {
	
	private static String NAMESPACE = "ui.jface.application.tellows.rendering.TellowsComments";

	public String renderAsText() {
		if (this.m_o!=null) {
			if (this.m_o instanceof ICall) {
				this.m_o = ((ICall)this.m_o).getCaller();
			}
			if (this.m_o instanceof ICaller) {
				IAttributeMap m = ((ICaller)(this.m_o)).getAttributes();
				if (m.contains("tellows.comments")) {
					return m.get("tellows.comments").getValue();
				}				
			}
		}
		return "";
	}

	public String getID() {
		return "TellowsComments".toLowerCase();
	}
	
	public String getNamespace() {
		return NAMESPACE;
	}

}
