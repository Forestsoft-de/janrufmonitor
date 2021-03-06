package de.janrufmonitor.ui.jface.application.journal.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.janrufmonitor.exception.Message;
import de.janrufmonitor.exception.PropagationFactory;
import de.janrufmonitor.repository.ICallManager;
import de.janrufmonitor.repository.types.ILocalRepository;
import de.janrufmonitor.repository.types.IRemoteRepository;
import de.janrufmonitor.runtime.IRuntime;
import de.janrufmonitor.runtime.PIMRuntime;
import de.janrufmonitor.ui.jface.application.AbstractAction;
import de.janrufmonitor.ui.jface.application.action.IAction;
import de.janrufmonitor.ui.jface.application.journal.Journal;
import de.janrufmonitor.ui.jface.application.journal.JournalConfigConst;
import de.janrufmonitor.ui.swt.DisplayManager;
import de.janrufmonitor.util.io.PathResolver;

public class OpenJournalAction extends AbstractAction {

	private class RemoteAction extends AbstractAction implements
			JournalConfigConst {

		private IRemoteRepository m_lr;

		public RemoteAction(IRemoteRepository lr) {
			super();
			this.m_lr = lr;
			this.setText(this.getI18nManager().getString(this.getNamespace(),
					"title", "label", this.getLanguage())
					+ "...");
		}

		public IRuntime getRuntime() {
			return OpenJournalAction.this.getRuntime();
		}

		public String getNamespace() {
			return m_lr.getNamespace();
		}

		public void run() {
			try {
				if (this.m_lr instanceof ICallManager) {
					OpenJournalAction.this.m_app.getApplication()
							.getConfiguration().setProperty(
									CFG_REPOSITORY,
									((ICallManager) this.m_lr)
											.getManagerID());
					OpenJournalAction.this.m_app.getApplication()
							.storeConfiguration();
				}

				getRuntime().getConfigurableNotifier().notifyByNamespace(
						this.m_lr.getNamespace());
				
				updateLastOpenJournalEntries((ICallManager)this.m_lr);

				OpenJournalAction.this.m_app.updateViews(true);

			} catch (Exception ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				PropagationFactory.getInstance()
						.fire(
								new Message(Message.ERROR, getNamespace(),
										"error", ex));
			}
		}
		
		public boolean isEnabled() {
			return (this.m_lr instanceof ICallManager && ((ICallManager) this.m_lr).isActive());
		}

		public String getID() {
			return "journal_new_journal_" + m_lr;
		}

	}

	private class LocalAction extends AbstractAction implements
			JournalConfigConst {

		private ILocalRepository m_lr;
	
		public LocalAction(ILocalRepository lr) {
			super();
			this.m_lr = lr;
			this.setText(this.getI18nManager().getString(this.getNamespace(),
					"title", "label", this.getLanguage())
					+ "...");
		}
	
		public IRuntime getRuntime() {
			return OpenJournalAction.this.getRuntime();
		}
	
		public String getNamespace() {
			return m_lr.getNamespace();
		}
	
		public void run() {
			try {
				FileDialog dialog = new FileDialog(new Shell(DisplayManager
						.getDefaultDisplay()), SWT.OPEN);
				dialog.setText(this.getI18nManager().getString(
						this.getNamespace(), "title", "label",
						this.getLanguage()));
				//dialog.setFilterPath(this.m_lr.getFile());
				String filter = getRuntime().getConfigManagerFactory().getConfigManager().getProperty(getNamespace(), "lastopeneddir");
				if (filter == null || filter.length() == 0 || !new File(filter).exists())
					filter = PathResolver.getInstance(getRuntime()).getDataDirectory();
				dialog.setFilterPath(filter);
				
				dialog.setFilterNames(new String[] { this.getI18nManager()
						.getString(this.getNamespace(), "title", "label",
								this.getLanguage())
						+ " (" + this.m_lr.getFileType() + ")" });
				dialog.setFilterExtensions(new String[] { this.m_lr
						.getFileType() });
	
				final String filename = dialog.open();
				if (filename == null)
					return;
	
				filter = new File(filename).getParentFile().getAbsolutePath();
				getRuntime().getConfigManagerFactory().getConfigManager().setProperty(getNamespace(), "lastopeneddir", filter);

				File f = new File(filename);
	
				if (f.exists()) {
					if (this.m_lr instanceof ICallManager) {
						OpenJournalAction.this.m_app.getApplication()
								.getConfiguration().setProperty(
										CFG_REPOSITORY,
										((ICallManager) this.m_lr)
												.getManagerID());
						OpenJournalAction.this.m_app.getApplication()
								.storeConfiguration();
					}
	
					this.m_lr.setFile(filename);
					getRuntime().getConfigurableNotifier().notifyByNamespace(
							this.m_lr.getNamespace());
					
					updateLastOpenJournalEntries((ICallManager)this.m_lr);
	
					OpenJournalAction.this.m_app.updateViews(true);
	
				}
	
			} catch (Exception ex) {
				this.m_logger.log(Level.SEVERE, ex.getMessage(), ex);
				PropagationFactory.getInstance()
						.fire(
								new Message(Message.ERROR, getNamespace(),
										"error", ex));
			}
		}
		
		
		public boolean isEnabled() {
			return (this.m_lr instanceof ICallManager && ((ICallManager) this.m_lr).isActive());
		}
	
		public String getID() {
			return "journal_new_journal_" + m_lr;
		}
	
	}

	private static String NAMESPACE = "ui.jface.application.journal.action.OpenJournalAction";

	private IRuntime m_runtime;

	public OpenJournalAction() {
		super();
		this.setText(this.getI18nManager().getString(this.getNamespace(),
				"title", "label", this.getLanguage()));
	}

	public IRuntime getRuntime() {
		if (this.m_runtime == null) {
			this.m_runtime = PIMRuntime.getInstance();
		}
		return this.m_runtime;
	}

	public String getID() {
		return "journal_open_journal";
	}

	public String getNamespace() {
		return NAMESPACE;
	}

	public IAction[] getSubActions() {
		List localCallManagers = getRuntime().getCallManagerFactory()
				.getTypedCallManagers(ILocalRepository.class);
		
		List remoteCallManagers = getRuntime().getCallManagerFactory()
		.getTypedCallManagers(IRemoteRepository.class);
		
		
		IAction[] actions = new IAction[localCallManagers.size() + (remoteCallManagers.size()>0 ? remoteCallManagers.size() : 0 )];

		for (int i = 0; i < localCallManagers.size(); i++) {
			actions[i] = new LocalAction((ILocalRepository) localCallManagers
					.get(i));
		}
		
		if (remoteCallManagers.size()>0)
			for (int i = 0; i < remoteCallManagers.size(); i++) {
				actions[i + localCallManagers.size()] = new RemoteAction((IRemoteRepository) remoteCallManagers
						.get(i));
			}

		return actions;
	}

	public boolean hasSubActions() {
		return true;
	}
	
	public void run() {	
		MessageDialog.openInformation(
				new Shell(DisplayManager.getDefaultDisplay()),
				"",
				this.getI18nManager().getString(getNamespace(), "nocms", "label", this.getLanguage()+this.getID())			
		);
	}
	
	private void updateLastOpenJournalEntries(ICallManager mgr) {
		List cms = new ArrayList();


		Properties config = getRuntime().getConfigManagerFactory().getConfigManager().getProperties(LastOpenJournalAction.NAMESPACE);
		String lastOpen = config.getProperty(JournalConfigConst.CFG_LASTOPEN, "");
		if (lastOpen.length()>0) {
			String[] locm = lastOpen.split(";");
			if (locm!=null && locm.length>0) {
				String l = null;
				for (int i=0;i<locm.length;i++) {
					l = locm[i];
					cms.add(l);
				}
			}			
		}
		if (mgr instanceof ILocalRepository) {
			String newcm = mgr.getManagerID()+"%"+((ILocalRepository)mgr).getFile();
			if (!cms.contains(newcm))
				cms.add(0,newcm);	
		}
		if (mgr instanceof IRemoteRepository) {
			String newcm = mgr.getManagerID();
			if (!cms.contains(newcm))
				cms.add(0,newcm);	
		}
		cms = cms.subList(0, Math.min(cms.size(), 5));
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<cms.size();i++) {
			sb.append(cms.get(i));
			sb.append(";");
		}
		config.setProperty(JournalConfigConst.CFG_LASTOPEN, sb.toString());
		getRuntime().getConfigManagerFactory().getConfigManager().setProperties(LastOpenJournalAction.NAMESPACE, config);
		getRuntime().getConfigurableNotifier().notifyByNamespace(Journal.NAMESPACE);
	}

}
