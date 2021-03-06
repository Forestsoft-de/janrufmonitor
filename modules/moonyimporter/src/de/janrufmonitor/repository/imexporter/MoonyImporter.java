package de.janrufmonitor.repository.imexporter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.janrufmonitor.framework.ICallFactory;
import de.janrufmonitor.framework.ICallList;
import de.janrufmonitor.framework.ICaller;
import de.janrufmonitor.framework.ICallerList;
import de.janrufmonitor.framework.ICip;
import de.janrufmonitor.framework.IMsn;
import de.janrufmonitor.framework.IName;
import de.janrufmonitor.framework.IJAMConst;
import de.janrufmonitor.framework.IPhonenumber;
import de.janrufmonitor.framework.i18n.II18nManager;
import de.janrufmonitor.repository.identify.Identifier;
import de.janrufmonitor.repository.imexport.ICallImporter;
import de.janrufmonitor.repository.imexport.IImExporter;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;

public class MoonyImporter implements ICallImporter {
	
	private String ID = "MoonyImporter";
	private String NAMESPACE = "repository.MoonyImporter";
	
	Logger m_logger;
	ICallList m_callList;
	II18nManager m_i18n;
	String m_language;
	String m_filename;
	
	private IRuntime m_runtime;
	
	private class MigratorThread implements Runnable {	

		private ICallList m_cl;
		private Properties m_call;
		private ICallFactory m_clf;

		public MigratorThread(ICallList cl, ICallerList c, Properties line) {
			this.m_cl = cl;
			this.m_call = line;
			this.m_clf = getRuntime().getCallFactory();
		}
		
		public MigratorThread(ICallList cl, Properties line) {
			this(cl, null , line);
		}

		public void run() {
				
			if (this.m_call==null) {
				m_logger.warning("Invalid migration entry.");
				return;
			}
			
			IPhonenumber p = getRuntime().getCallerFactory().createPhonenumber(
				this.m_call.getProperty("CallerNumber", " ").substring(1)
			);
			
			ICaller c = null;
			c = Identifier.identifyDefault(getRuntime(), p);
			if (c!=null) {
				IName n = c.getName();
				n.setFirstname(
					this.m_call.getProperty(
						"CallerDescription",	
						""
					)	
				);
				c.setName(n);
			}


			IMsn msn = this.m_clf.createMsn(
				this.m_call.getProperty(
					"TargetNumber",	
					""
				), 
			"");
			
			ICip cip = this.m_clf.createCip("999", "");

			String time = 
				this.m_call.getProperty(
					"LTime",	
					"0"
				);
			
			Date date = new Date(
				Long.parseLong(
					time + (time.length()==10 ? "000" : "")
				)
			);
			
			this.m_cl.add(this.m_clf.createCall(c, msn, cip, date));
		}
	}

	public MoonyImporter() {
		m_logger = LogManager.getLogManager().getLogger(IJAMConst.DEFAULT_LOGGER);
		m_i18n = getRuntime().getI18nManagerFactory().getI18nManager();
		m_language = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(IJAMConst.GLOBAL_NAMESPACE, IJAMConst.GLOBAL_LANGUAGE);
	}

	public String getID() {
		return this.ID;
	}

	public int getMode() {
		return IImExporter.CALL_MODE;
	}

	public String getFilterName() {
		return this.m_i18n.getString(this.NAMESPACE, "filtername", "label", this.m_language);
	}

	public String getExtension() {
		return "*.inf";
	}

	public void setFilename(String filename) {
		this.m_filename = filename;
	}

	public ICallList doImport() {
		Date startDate = new Date();
	    File db = new File(m_filename);
	    
	    int size = (int)db.length() / 64;
	    size = Math.abs(size);
	    
		ThreadGroup g = new ThreadGroup("MigratorThreadGroup");
		m_callList = getRuntime().getCallFactory().createCallList(size);
		
		try {
		 FileReader dbReader = new FileReader(db);
		 BufferedReader bufReader = new BufferedReader(dbReader);
		 int i=0;
		 String line = null;
		 StringBuffer newCall = null;
		 Properties callObj = null;
		 
		 while (bufReader.ready()) {
			 line = bufReader.readLine();
			 if (line.indexOf("[#")==0) {
			 	// found new entry
			 	newCall = new StringBuffer();
			 	callObj = new Properties();
			 	ByteArrayInputStream in = null;
			 	do {
			 		line = bufReader.readLine();
			 		newCall.append(line);
			 		newCall.append(IJAMConst.CRLF);
			 	}
			 	while (bufReader.ready() && line.trim().length()>0);
			 	
			 	in = new ByteArrayInputStream(newCall.toString().getBytes());
			 	callObj.load(in);
			 	
			 	try {
			 		Thread t = new Thread(g, new MigratorThread(this.m_callList, callObj));
			 		t.setName("MigratorThread#"+ i++);
			 		t.start();
			 		if (g.activeCount()>100) {
			 			Thread.sleep(1000);
			 		}
			 	} catch (Exception ex) {
					this.m_logger.severe("Unexpected error during migration: "+ex);
			 	}
			 }
		 }
		 bufReader.close();
		 dbReader.close();
		} catch (FileNotFoundException ex) {
		 this.m_logger.warning("Cannot find call backup file " + m_filename);
		 return this.m_callList;
		} catch (IOException ex) {
		 this.m_logger.severe("IOException on file " + m_filename);
		 return this.m_callList;
		}
		
		while (g.activeCount()>0) {
			try {
				Thread.sleep(1000);
				this.m_logger.info("Waiting for "+g.activeCount()+" Migrator threads.");
			} catch (InterruptedException e) {
				this.m_logger.severe(e.getMessage());
			}
		}
		
		Date endDate = new Date();
		this.m_logger.info("Successfully imported call file " + m_filename);
		this.m_logger.info("Found " + new Integer(this.m_callList.size()).toString() + " call items in " + new Float((endDate.getTime() - startDate.getTime()) / 1000).toString() + " secs.");
		return m_callList;
	}

	public int getType() {
		return IImExporter.IMPORT_TYPE;
	}

	private IRuntime getRuntime() {
		if(this.m_runtime==null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}
	
	protected II18nManager getI18nManager() {
		if (this.m_i18n==null) {
			this.m_i18n = this.getRuntime().getI18nManagerFactory().getI18nManager();
		}
		return this.m_i18n;
	}
	
	protected String getLanguage() {
		if (this.m_language==null) {
			this.m_language = 
				this.getRuntime().getConfigManagerFactory().getConfigManager().getProperty(
					IJAMConst.GLOBAL_NAMESPACE,
					IJAMConst.GLOBAL_LANGUAGE
				);
		}
		return this.m_language;
	}
	
	public String getNamespace() {
		return NAMESPACE;
	}
}
