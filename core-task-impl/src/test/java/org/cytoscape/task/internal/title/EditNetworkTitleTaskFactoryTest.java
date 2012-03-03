package org.cytoscape.task.internal.title;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.undo.UndoSupport;
import org.junit.Test;


public class EditNetworkTitleTaskFactoryTest {
	@Test
	public void testGetTaskIterator() {
		CyNetwork net = mock(CyNetwork.class);
		CyNetworkManager netMgr = mock(CyNetworkManager.class);
		
		CyRow r1 =  mock(CyRow.class);
		when(net.getRow(net)).thenReturn(r1);
		when(r1.get("name", String.class)).thenReturn("title");

		UndoSupport undoSupport = mock(UndoSupport.class);

		EditNetworkTitleTaskFactory factory = new EditNetworkTitleTaskFactory(undoSupport,netMgr);
		factory.setNetwork(net);
		
		TaskIterator ti = factory.createTaskIterator();
		assertNotNull(ti);
		
		assertTrue( ti.hasNext() );
		Task t = ti.next();
		assertNotNull( t );		
	}
}
