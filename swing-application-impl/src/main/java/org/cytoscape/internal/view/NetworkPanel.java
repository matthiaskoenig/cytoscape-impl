/*
 File: NetworkPanel.java

 Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package org.cytoscape.internal.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetSelectedNetworksEvent;
import org.cytoscape.application.events.SetSelectedNetworksListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.internal.task.TaskFactoryTunableAction;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.DynamicTaskFactoryProvisioner;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.edit.EditNetworkTitleTaskFactory;
import org.cytoscape.util.swing.JTreeTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkPanel extends JPanel implements TreeSelectionListener, SetSelectedNetworksListener,
		NetworkAddedListener, NetworkViewAddedListener, NetworkAboutToBeDestroyedListener,
		NetworkViewAboutToBeDestroyedListener, RowsSetListener, SessionAboutToBeSavedListener, SessionLoadedListener {

	private static final String NETWORK_PANEL_TABLE = "CY_NETWORK_LIST_UI";
	private static final String NETWORK_SUID_COLUMN = "network.SUID";
	private static final String NETWORK_ORDER_COLUMN = "network_order";

	private final static long serialVersionUID = 1213748836763243L;

	private static final Logger logger = LoggerFactory.getLogger(NetworkPanel.class);

	static final Color FONT_COLOR = new Color(20, 20, 20);
	private static final int TABLE_ROW_HEIGHT = 16;
	private static final Dimension PANEL_SIZE = new Dimension(400, 700);

	private final JTreeTable treeTable;
	private final NetworkTreeNode root;
	private JPanel navigatorPanel;
	private JSplitPane split;

	private final NetworkTreeTableModel treeTableModel;
	private final CyApplicationManager appMgr;
	final CyNetworkManager netMgr;
	private final CyNetworkViewManager netViewMgr;
	private final CyTableManager tblMgr;
	private final CyTableFactory tblFactory;
	private final DialogTaskManager taskMgr;
	private final DynamicTaskFactoryProvisioner factoryProvisioner;

	private final JPopupMenu popup;
	private JMenuItem editRootNetworTitle;
	
	private final Map<TaskFactory, JMenuItem> popupMap;
	private final Map<TaskFactory, CyAction> popupActions;
	private HashMap<JMenuItem, Double> actionGravityMap;
	private final Map<Object, TaskFactory> provisionerMap;
	
	private final Map<CyTable, CyNetwork> nameTables;
	private final Map<CyTable, CyNetwork> nodeEdgeTables;
	private final Map<Long, NetworkTreeNode> treeNodeMap;
	private final Map<CyNetwork, NetworkTreeNode> network2nodeMap;

	private boolean ignoreTreeSelectionEvents;

	private final EditNetworkTitleTaskFactory rootNetworkTitleEditor;
	private final JPopupMenu rootPopupMenu;	
	private CyRootNetwork selectedRoot;
	private Set<CyRootNetwork> selectedRootSet;

	/**
	 * 
	 * @param appMgr
	 * @param netMgr
	 * @param netViewMgr
	 * @param bird
	 * @param taskMgr
	 */
	public NetworkPanel(final CyApplicationManager appMgr,
						final CyNetworkManager netMgr,
						final CyNetworkViewManager netViewMgr,
						final BirdsEyeViewHandler bird,
						final DialogTaskManager taskMgr,
						final CyTableManager tblMgr,
						final CyTableFactory tblFactory,
						final DynamicTaskFactoryProvisioner factoryProvisioner,
						final EditNetworkTitleTaskFactory networkTitleEditor) {
		super();

		this.treeNodeMap = new HashMap<Long, NetworkTreeNode>();
		this.provisionerMap = new HashMap<Object, TaskFactory>();
		this.appMgr = appMgr;
		this.netMgr = netMgr;
		this.netViewMgr = netViewMgr;
		this.taskMgr = taskMgr;
		this.tblMgr = tblMgr;
		this.tblFactory = tblFactory;
		this.factoryProvisioner = factoryProvisioner;
		
		root = new NetworkTreeNode("Network Root", null);
		treeTableModel = new NetworkTreeTableModel(this, root);
		treeTable = new JTreeTable(treeTableModel);
		initialize();

		this.actionGravityMap = new HashMap<JMenuItem, Double>();
		
		// create and populate the popup window
		popup = new JPopupMenu();
		popupMap = new WeakHashMap<TaskFactory, JMenuItem>();
		popupActions = new WeakHashMap<TaskFactory, CyAction>();
		nameTables = new WeakHashMap<CyTable, CyNetwork>();
		nodeEdgeTables = new WeakHashMap<CyTable, CyNetwork>();
		this.network2nodeMap = new WeakHashMap<CyNetwork, NetworkTreeNode>();

		
		setNavigator(bird.getBirdsEyeView());

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : treeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = treeTable.getInputMap();
				map.remove(listener);
				treeTable.setInputMap(WHEN_FOCUSED, map);
				treeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
			}
		}
		

		this.rootNetworkTitleEditor = networkTitleEditor;
		rootPopupMenu = new JPopupMenu();
		editRootNetworTitle = new JMenuItem("Rename Network Collection...");
		editRootNetworTitle.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				taskMgr.execute(rootNetworkTitleEditor.createTaskIterator(selectedRoot));
			}
		});
		rootPopupMenu.add(editRootNetworTitle);
		
		JMenuItem selectAllSubNetsMenuItem = new JMenuItem("Select All Networks");
		selectAllSubNetsMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllSubnetwork();
			}
		});
		rootPopupMenu.add(selectAllSubNetsMenuItem);
	}

	protected void initialize() {
		setLayout(new BorderLayout());
		setPreferredSize(PANEL_SIZE);
		setSize(PANEL_SIZE);

		treeTable.getTree().addTreeSelectionListener(this);
		treeTable.getTree().setRootVisible(false);

		ToolTipManager.sharedInstance().registerComponent(treeTable);

		treeTable.getTree().setCellRenderer(new TreeCellRenderer(treeTable));
		treeTable.setBackground(Color.white);
		treeTable.setSelectionBackground(new Color(200, 200, 200, 150));

		treeTable.getColumn("Network").setPreferredWidth(250);
		treeTable.getColumn("Nodes").setPreferredWidth(45);
		treeTable.getColumn("Edges").setPreferredWidth(45);

		treeTable.setBackground(Color.WHITE);
		treeTable.setRowHeight(TABLE_ROW_HEIGHT);
		treeTable.setForeground(FONT_COLOR);
		treeTable.setSelectionForeground(FONT_COLOR);
		treeTable.setCellSelectionEnabled(false);
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setSelectionModel(new DefaultTreeSelectionModel());

		navigatorPanel = new JPanel();
		navigatorPanel.setLayout(new BorderLayout());
		navigatorPanel.setPreferredSize(new Dimension(280, 280));
		navigatorPanel.setSize(new Dimension(280, 280));
		navigatorPanel.setBackground(Color.white);

		JScrollPane scroll = new JScrollPane(treeTable);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, navigatorPanel);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setResizeWeight(1);
		split.setDividerLocation(400);

		add(split);

		// this mouse listener listens for the right-click event and will show
		// the pop-up window when that occurrs
		treeTable.addMouseListener(new PopupListener());
	}

	public void addTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		addFactory(factory, props);
	}

	public void removeTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(factory);
	}

	public void addNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewTaskFactory(final NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewTaskFactory(NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void setNavigator(final Component comp) {
		this.navigatorPanel.removeAll();
		this.navigatorPanel.add(comp, BorderLayout.CENTER);
	}

	/**
	 * This is used by Session writer.
	 * @return
	 */
	public JTreeTable getTreeTable() {
		return treeTable;
	}

	public JPanel getNavigatorPanel() {
		return navigatorPanel;
	}

	// // Event handlers // //
	
	@Override
	public void handleEvent(final NetworkAboutToBeDestroyedEvent nde) {
		final CyNetwork net = nde.getNetwork();
		logger.debug("Network about to be destroyed: " + net);
		removeNetwork(net);
	}

	@Override
	public void handleEvent(final NetworkAddedEvent e) {
		final CyNetwork net = e.getNetwork();
		logger.debug("Network added: " + net);
		addNetwork(net);
	}

	@Override
	public void handleEvent(final RowsSetEvent e) {
		final Collection<RowSetRecord> payload = e.getPayloadCollection();
		if (payload.size() == 0)
			return;

		final RowSetRecord record = e.getPayloadCollection().iterator().next();
		if (record == null)
			return;
		
		final CyTable table = e.getSource();
		final CyNetwork network = nameTables.get(table);
		
		// Case 1: Network name/title updated
		if (network != null && record.getColumn().equals(CyNetwork.NAME)) {
			final CyRow row = payload.iterator().next().getRow();
			final String newTitle = row.get(CyNetwork.NAME, String.class);
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			final String oldTitle = treeTableModel.getValueAt(node, 0).toString();

			if (newTitle.equals(oldTitle) == false) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						treeTableModel.setValueAt(newTitle, node, 0);
						treeTable.repaint();
					}
				});
			}
			return;
		}

		final CyNetwork updateSelected = nodeEdgeTables.get(table);

		// Case 2: Selection updated.
		if (updateSelected != null && record.getColumn().equals(CyNetwork.SELECTED)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					treeTable.repaint();
				}
			});
		}
	}

	@Override
	public void handleEvent(final SetSelectedNetworksEvent e) {
		updateNetworkTreeSelection();
	}

	@Override
	public void handleEvent(final NetworkViewAboutToBeDestroyedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view about to be destroyed: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null) {
					node.setNodeColor(Color.red);
					treeTable.repaint();
				}
			}
		});
	}

	@Override
	public void handleEvent(final NetworkViewAddedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view added to NetworkPanel: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null) {
					node.setNodeColor(Color.black);
					treeTable.repaint();
				}
			}
		});
	}
	
	@Override
	public void handleEvent(final SessionLoadedEvent e) {
		// Restore the network collections order
		final Set<CyNetwork> networks = e.getLoadedSession().getNetworks();
		
		if (networks.size() < 2)
			return; // No need to sort 1 network, of course
		
		final List<CyNetwork> sortedNetworks = new ArrayList<CyNetwork>(networks);
		final CyTable tbl = getNetworkPanelTable();
		
		Collections.sort(sortedNetworks, new Comparator<CyNetwork>() {
			@Override
			public int compare(final CyNetwork n1, final CyNetwork n2) {
				try {
					int o1 = -1;
					int o2 = -1;
					final Collection<CyRow> rows1 = tbl.getMatchingRows(NETWORK_SUID_COLUMN, n1.getSUID());
					final Collection<CyRow> rows2 = tbl.getMatchingRows(NETWORK_SUID_COLUMN, n2.getSUID());
					final CyRow r1 = rows1.isEmpty() ? null : rows1.iterator().next();
					final CyRow r2 = rows2.isEmpty() ? null : rows2.iterator().next();
					
					if (r1 != null && r1.isSet(NETWORK_ORDER_COLUMN))
						o1 = r1.get(NETWORK_ORDER_COLUMN, Integer.class);
					if (r2 != null && r2.isSet(NETWORK_ORDER_COLUMN))
						o2 = r2.get(NETWORK_ORDER_COLUMN, Integer.class);
					
					if (o1 < o2)
						return -1;
					else if (o1 > o2)
						return 1;
				} catch (final Exception e) {
					logger.error("Cannot sort networks", e);
				}
				
				return 0;
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// TODO: find a better way of doing it--one that does not require rebuilding the tree
				nameTables.clear();
				nodeEdgeTables.clear();
				network2nodeMap.clear();
				treeNodeMap.clear();
				
				ignoreTreeSelectionEvents = true;
				root.removeAllChildren();
				treeTable.getTree().updateUI();
				treeTable.repaint();
				ignoreTreeSelectionEvents = false;
				
				for (final CyNetwork n : sortedNetworks)
					addNetwork(n);
					
				updateNetworkTreeSelection();
			}
		});
		
		// Delete the table
		tblMgr.deleteTable(tbl.getSUID());
	}

	@Override
	public void handleEvent(final SessionAboutToBeSavedEvent e) {
		// Save the network orders
		if (netMgr.getNetworkSet().size() < 2)
			return; // No need save the orders, if there is only 1 network (or none)
		
		final CyTable tbl = getNetworkPanelTable();
		
		// Save the orders
		final JTree tree = treeTable.getTree();
		
		for (final Entry<CyNetwork, NetworkTreeNode> entry : network2nodeMap.entrySet()) {
			final CyNetwork net = entry.getKey();
			final NetworkTreeNode node = entry.getValue();
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				final int i = tree.getRowForPath(tp);
				
				final CyRow row = tbl.getRow(net.getSUID());
				row.set(NETWORK_SUID_COLUMN, net.getSUID());
				row.set(NETWORK_ORDER_COLUMN, i);
			}
		}
	}
	
	/**
	 * This method highlights a network in the NetworkPanel.
	 */
	@Override
	public void valueChanged(final TreeSelectionEvent e) {
		if (ignoreTreeSelectionEvents)
			return;

		final JTree tree = treeTable.getTree();

		// Sets the "current" network based on last node in the tree selected
		final NetworkTreeNode node = (NetworkTreeNode) tree.getLastSelectedPathComponent();
		
		if (node == null || node.getUserObject() == null)
			return;

		CyNetwork cn = node.getNetwork();
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
	
		/*
		if (cn instanceof CyRootNetwork) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			CyRootNetwork root = (CyRootNetwork) cn; 
					//((NetworkTreeNode) node.getFirstChild()).getNetwork()).getRootNetwork();

			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			// Determine the current network
			if (!selectedNetworks.isEmpty())
				cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		} else { */
			// Regular multiple networks selection...
			try {
				// Create a list of all selected networks
				for (int i = tree.getMinSelectionRow(); i <= tree.getMaxSelectionRow(); i++) {
					NetworkTreeNode tn = (NetworkTreeNode) tree.getPathForRow(i).getLastPathComponent();
					
					if (tn != null && tn.getUserObject() != null && tree.isRowSelected(i))
						selectedNetworks.add(tn.getNetwork());
				}
			} catch (Exception ex) {
				logger.error("Error creating the list of selected networks", ex);
			}
	//	}
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}
	
	// // Private Methods // //
	
	private void addNetwork(final CyNetwork network) {
		// first see if it is not in the tree already
		if (this.network2nodeMap.get(network) == null) {
			NetworkTreeNode parentTreeNode = null;
			CyRootNetwork parentNetwork = null;
			
			// In current version, ALL networks are created as Subnetworks.
			// So, this should be always true.
			if (network instanceof CySubNetwork) {
				parentNetwork = ((CySubNetwork) network).getRootNetwork();
				parentTreeNode = treeNodeMap.get(parentNetwork.getSUID());
			}

			if (parentTreeNode == null){
				final String rootNetName = parentNetwork.getRow(parentNetwork).get(CyNetwork.NAME, String.class);
				parentTreeNode = new NetworkTreeNode(rootNetName, parentNetwork);
				nameTables.put(parentNetwork.getDefaultNetworkTable(), parentNetwork);
				network2nodeMap.put(parentNetwork, parentTreeNode);
			}

			// Actual tree node for this network
			String netName = network.getRow(network).get(CyNetwork.NAME, String.class);

			if (netName == null) {
				logger.error("Network name is null--SUID=" + network.getSUID());
				netName = "? (SUID: " + network.getSUID() + ")";
			}

			final NetworkTreeNode dmtn = new NetworkTreeNode(netName, network);
			network2nodeMap.put(network, dmtn);
			parentTreeNode.add(dmtn);

			if (treeNodeMap.values().contains(parentTreeNode) == false)
				root.add(parentTreeNode);

			// Register top-level node to map
			if (parentNetwork != null)
				treeNodeMap.put(parentNetwork.getSUID(), parentTreeNode);

			if (netViewMgr.viewExists(network))
				dmtn.setNodeColor(Color.black);

			treeNodeMap.put(network.getSUID(), dmtn);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ignoreTreeSelectionEvents = true;
					// apparently this doesn't fire valueChanged
					treeTable.getTree().collapsePath(new TreePath(new TreeNode[] { root }));
					
					treeTable.getTree().updateUI();
					final TreePath path = new TreePath(dmtn.getPath());
					
					treeTable.getTree().expandPath(path);
					treeTable.getTree().scrollPathToVisible(path);
					treeTable.doLayout();
					
					ignoreTreeSelectionEvents = false;
				}
			});
		}
		
		nameTables.put(network.getDefaultNetworkTable(), network);
		nodeEdgeTables.put(network.getDefaultNodeTable(), network);
		nodeEdgeTables.put(network.getDefaultEdgeTable(), network);
	}
	
	/**
	 * Remove a network from the panel.
	 */
	private void removeNetwork(final CyNetwork network) {
		nameTables.values().removeAll(Collections.singletonList(network));
		nodeEdgeTables.values().removeAll(Collections.singletonList(network));
		treeNodeMap.remove(network.getSUID());
		final NetworkTreeNode node = network2nodeMap.remove(network);
		
		if (node == null)
			return;
		
		final List<NetworkTreeNode> removedChildren = new ArrayList<NetworkTreeNode>();
		final Enumeration<?> children = node.children();
		
		if (children.hasMoreElements()) {
			while (children.hasMoreElements())
				removedChildren.add((NetworkTreeNode) children.nextElement());
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				
				for (final NetworkTreeNode child : removedChildren) {
					child.removeFromParent();
					root.add(child);
				}
				
				final NetworkTreeNode parentNode = (NetworkTreeNode) node.getParent();
				node.removeFromParent();

				if (parentNode.isLeaf()) {
					// Remove from root node
					final CyNetwork parentNet = parentNode.getNetwork();
					nameTables.values().removeAll(Collections.singletonList(parentNet));
					nodeEdgeTables.values().removeAll(Collections.singletonList(parentNet));
					network2nodeMap.remove(parentNet);
					treeNodeMap.remove(parentNet.getSUID());
					
					parentNode.removeFromParent();
				}
				
				treeTable.getTree().updateUI();
				treeTable.repaint();
				
				ignoreTreeSelectionEvents = false;
			}
		});
	}

	/**
	 * Update selected row.
	 */
	private final void updateNetworkTreeSelection() {
		final List<TreePath> paths = new ArrayList<TreePath>();
		final List<CyNetwork> selectedNetworks = appMgr.getSelectedNetworks();
		
		for (final CyNetwork network : selectedNetworks) {
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				paths.add(tp);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				treeTable.getTree().getSelectionModel().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
				ignoreTreeSelectionEvents = false;
		
				int maxRow = 0;
		
				for (final TreePath tp : paths) {
					final int row = treeTable.getTree().getRowForPath(tp);
					maxRow = Math.max(maxRow, row);
				}
				
				final int row = maxRow;
		
				treeTable.getTree().scrollRowToVisible(row);
				treeTable.repaint();
			}
		});
	}

	private void selectAllSubnetwork(){
		if (selectedRootSet == null)
			return;
		
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
		CyNetwork cn = null;
		NetworkTreeNode node = null;
		
		for (final CyRootNetwork root : selectedRootSet) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			node = this.network2nodeMap.get(root);
			
			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			cn = root;
		}
		
		// Determine the current network
		if (!selectedNetworks.isEmpty())
			cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addFactory(TaskFactory factory, Map props) {
		CyAction action;
		if ( props.containsKey("enableFor") )
			action = new TaskFactoryTunableAction(taskMgr, factory, props, appMgr, netViewMgr);
		else
			action = new TaskFactoryTunableAction(taskMgr, factory, props);

		final JMenuItem item = new JMenuItem(action);

		Double gravity = 10.0;
		if (props.containsKey(ServiceProperties.MENU_GRAVITY)){
			gravity = Double.valueOf(props.get(ServiceProperties.MENU_GRAVITY).toString());
		}
		
		this.actionGravityMap.put(item, gravity);
		
		popupMap.put(factory, item);
		popupActions.put(factory, action);
		int menuIndex = getMenuIndexByGravity(item);
		popup.insert(item, menuIndex);
		popup.addPopupMenuListener(action);
	}

	private int getMenuIndexByGravity(JMenuItem item) {
		Double gravity = this.actionGravityMap.get(item);		
		Double gravityX;
		for (int i=0; i < popup.getComponentCount(); i++ ){
			gravityX = this.actionGravityMap.get(popup.getComponent(i));
			if (gravity < gravityX){
				return i;
			}
		}
		
		return popup.getComponentCount();
	}
	
	private void removeFactory(TaskFactory factory) {
		JMenuItem item = popupMap.remove(factory);
		if (item != null)
			popup.remove(item);
		CyAction action = popupActions.remove(factory);
		if (action != null)
			popup.removePopupMenuListener(action);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends CyNetwork> Set<T> getSelectionNetworks(Class<T> type) {
		final Set<T> nets = new LinkedHashSet<T>();
		final JTree tree = treeTable.getTree();
		final TreePath[] selectionPaths = tree.getSelectionPaths();
		
		if (selectionPaths != null) {
			for (final TreePath tp : selectionPaths) {
				final CyNetwork n = ((NetworkTreeNode) tp.getLastPathComponent()).getNetwork();
				
				if (n != null && type.isAssignableFrom(n.getClass()))
					nets.add((T) n);
			}
		}
		
		return nets;
	}
	
	private CyTable getNetworkPanelTable() {
		CyTable tbl = null;
		// First check if it already exists
		final Set<CyTable> globalTables = tblMgr.getGlobalTables();
		
		for (final CyTable t : globalTables) {
			if (NETWORK_PANEL_TABLE.equals(t.getTitle())) {
				tbl = t;
				break;
			}
		}
		
		if (tbl == null) {
			// Create the table, if it doesn't exist
			tbl = tblFactory.createTable(NETWORK_PANEL_TABLE, CyIdentifiable.SUID, Long.class, false, true);
			tbl.setSavePolicy(SavePolicy.SESSION_FILE);
			tbl.createColumn(NETWORK_SUID_COLUMN, Long.class, true);
			tbl.createColumn(NETWORK_ORDER_COLUMN, Integer.class, true, -1);
			tblMgr.addTable(tbl);
		}
		
		return tbl;
	}
	
	// // Private Classes // //
	
	/**
	 * This class listens to mouse events from the TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	private final class PopupListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		// On Windows, popup is triggered by mouse release, not press 
		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * if the mouse press is of the correct type, this function will maybe
		 * display the popup
		 */
		private final void maybeShowPopup(final MouseEvent e) {
			// Ignore if not valid trigger.
			if (!e.isPopupTrigger())
				return;

			// get the row where the mouse-click originated
			final int row = treeTable.rowAtPoint(e.getPoint());
			if (row == -1)
				return;

			final JTree tree = treeTable.getTree();
			final TreePath treePath = tree.getPathForRow(row);
			Long networkID = null;
			
			try {
				final CyNetwork clickedNet = ((NetworkTreeNode) treePath.getLastPathComponent()).getNetwork();
				
				if (clickedNet instanceof CyRootNetwork) {
					networkID = null;
					selectedRoot = (CyRootNetwork) clickedNet;
				} else {
					networkID = clickedNet.getSUID();
					selectedRoot = null;
				}
			} catch (NullPointerException npe) {
				// The tree root does not represent a network, ignore it.
				return;
			}

			if (networkID != null) {
				final CyNetwork network = netMgr.getNetwork(networkID);
				
				if (network != null) {
					// if the network is not selected, select it
					final List<CyNetwork> selectedList = appMgr.getSelectedNetworks();
					
					if (selectedList == null || !selectedList.contains(network)) {
						appMgr.setCurrentNetwork(network);
						appMgr.setSelectedNetworks(Arrays.asList(new CyNetwork[]{ network }));
						final Collection<CyNetworkView> netViews = netViewMgr.getNetworkViews(network);
						appMgr.setSelectedNetworkViews(new ArrayList<CyNetworkView>(netViews));
					}
					
					// Always repaint, because the rendering of the tree selection
					// may be out of sync with the AppManager one
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							treeTable.repaint();
						}
					});
					
					// enable/disable any actions based on state of system
					for (CyAction action : popupActions.values())
						action.updateEnableState();

					// then popup menu
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			} else if (selectedRoot != null) {
				// if the right-clicked root-network is not selected, select it (other selected items will be unselected)
				selectedRootSet = getSelectionNetworks(CyRootNetwork.class);
				
				if (!selectedRootSet.contains(selectedRoot)) {
					final NetworkTreeNode node = network2nodeMap.get(selectedRoot);
					
					if (node != null) {
						appMgr.setCurrentNetwork(null);
						appMgr.setSelectedNetworks(null);
						appMgr.setSelectedNetworkViews(null);
						
						selectedRootSet = Collections.singleton(selectedRoot);
						final TreePath tp = new TreePath(new NetworkTreeNode[]{ root, node });
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								ignoreTreeSelectionEvents = true;
								tree.getSelectionModel().setSelectionPaths(new TreePath[]{ tp });
								treeTable.repaint();
								ignoreTreeSelectionEvents = false;
							}
						});
					}
				}
				
				editRootNetworTitle.setEnabled(selectedRootSet.size() == 1);
				rootPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
}
