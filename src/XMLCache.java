import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XMLCache {
	public XMLOutputter outputter;
	public File xmlFile;
	public Document doc;
	
	public XMLCache(String xmlFileName) throws IOException {
		try {
			xmlFile = new File(xmlFileName);
			if (xmlFile.exists()) {
				org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
				builder.setIgnoringBoundaryWhitespace(true);
				builder.setIgnoringElementContentWhitespace(true);
				doc = builder.build(xmlFile);
			}
		} catch (Exception e) {} 

		if (doc == null) doc = new Document(new Element("root"));
		outputter = new XMLOutputter();
		Format xmlOutputFormat = outputter.getFormat();
		xmlOutputFormat.setIndent("  ");
		xmlOutputFormat.setLineSeparator("\n");
		outputter.setFormat(xmlOutputFormat);		
	}


	/**
	 * Saves XML cache back into a file
	 * @throws IOException
	 */
	public void saveCache() throws IOException {
			outputter.output(doc, new FileOutputStream(xmlFile, false));
	}
}
