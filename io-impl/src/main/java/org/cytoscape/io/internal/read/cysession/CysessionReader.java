package org.cytoscape.io.internal.read.cysession;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.cytoscape.io.internal.read.AbstractPropertyReader;
import org.cytoscape.property.session.Cysession;
import org.cytoscape.work.TaskMonitor;

public class CysessionReader extends AbstractPropertyReader {

	private static final String CYSESSION_PACKAGE = Cysession.class.getPackage().getName();

	public CysessionReader(InputStream is) {
		super(is);
	}

	public void run(TaskMonitor tm) throws Exception {

		// No idea why, but ObjectFactory doesn't get picked up in the default
		// Thread.currentThread().getContextClassLoader() classloader, whereas 
		// that approach works fine for bookmarks.  Anyway, just force the issue
		// by getting this classloader.
		final JAXBContext jaxbContext = JAXBContext.newInstance(CYSESSION_PACKAGE,
		                                                        getClass().getClassLoader());

		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		propertyObject = (Cysession) unmarshaller.unmarshal(inputStream);
	}
}
