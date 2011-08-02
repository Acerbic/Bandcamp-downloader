package dloader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class to store data from previously downloaded pages.
 * @author A.Cerbic
 */
public class XMLCache {
	/**
	 * handles formatting for saving as a file
	 */
	private XMLOutputter outputter;
	
	private File xmlFile;
	
	public Document doc;
	
/**
 * Loads file and parses it into org.jdom.Document
 * If document cannot be read for any reason, new empty valid one is created 
 * (the file will be created when saveCache() is called next time).
 * @param xmlFileName - cache file name
 * @throws IllegalArgumentException if file name is not valid
 */
	public XMLCache(String xmlFileName) {
		Logger l = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		if (xmlFileName.isEmpty()) 
			throw new IllegalArgumentException("Cache file name cannot be empty");
		
		try {
			xmlFile = new File(xmlFileName);
			if (xmlFile.exists()) {
				org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
				builder.setIgnoringBoundaryWhitespace(true);
				builder.setIgnoringElementContentWhitespace(true);
				doc = builder.build(xmlFile);
			} else {
				try {
					xmlFile.createNewFile();
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				}
			}
		} catch (IOException e) {
			l.log(Level.WARNING, String.format("Error reading cache file <%s>%n", xmlFileName), e);
		} catch (JDOMException e) {
			l.log(Level.WARNING, String.format("Error parsing cache file <%s>%n", xmlFileName), e);
		} 

		if (doc == null) doc = new Document(new Element("root"));
		outputter = new XMLOutputter();
		Format xmlOutputFormat = outputter.getFormat();
		xmlOutputFormat.setIndent("  ");
		xmlOutputFormat.setLineSeparator(System.getProperty("line.separator"));
		outputter.setFormat(xmlOutputFormat);		
	}

	/**
	 * Saves XML cache back into a file
	 * @throws IOException - if problems occur.
	 */
	public void saveCache() throws IOException {
			outputter.output(doc, new FileOutputStream(xmlFile, false));
	}
}
