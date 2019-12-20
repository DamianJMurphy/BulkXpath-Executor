/*
 Copyright 2019  Damian Murphy <murff@warlock.org>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.warlock.bulkxpathexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.warlock.util.CfHNamespaceContext;
import org.xml.sax.InputSource;

/**
 *
 * @author Damian Murphy
 */
public class BulkXpathExecutor {

    private static final String USAGE = "Usage: java -jar BulkXpathExecutor.jar -p pathsfile [ -r datafile ] [ -m ] [ -M ] [ -f ] [ -t ] [ -o outputfile ] [ -e errorfile ] [ -x extension ] [ -X extension ] [ documentfile | - ]";
    private HashMap<String,DescribedXPath> expressions = new HashMap<>();
    private HashMap<String,ArrayList<String>> substitutions = null;
    private NamespaceContext nhsdNS = CfHNamespaceContext.getXMLNamespaceContext();
    private static final SimpleDateFormat ISO8601TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat ISO8601DATE = new SimpleDateFormat("yyyy-MM-dd");
    
    private OutputManager outputManager = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String paths = null;
        String data = null;
        ArrayList<String> doc = new ArrayList<>();
        String outfile = null;
        OutputManager om = new OutputManager();
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].contentEquals("-p")) {
                    ++i;
                    paths = args[i];
                    continue;
                }
                if (args[i].contentEquals("-r")) {
                    ++i;
                    data = args[i];
                    continue;
                }
                if (args[i].contentEquals("-o")) {
                    ++i;
                    om.setOutputFile(args[i]);
                    continue;
                }            
                if (args[i].contentEquals("-e")) {
                    ++i;
                    om.setErrorFile(args[i]);
                    continue;
                }            
                if (args[i].contentEquals("-x")) {
                    ++i;
                    om.setOutputExtension(args[i]);
                    continue;
                }            
                if (args[i].contentEquals("-X")) {
                    ++i;
                    om.setErrorExtension(args[i]);
                    continue;
                }            
                if (args[i].contentEquals("-m")) {
                    om.setInMemoryOutput();
                    continue;
                }            
                if (args[i].contentEquals("-M")) {
                    om.setInMemoryError();
                    continue;
                }            
                if (args[i].contentEquals("-f")) {
                    om.setPrependFilenameToError(true);
                    continue;
                }            
                if (args[i].contentEquals("-t")) {
                    om.setTimestampError(true);
                    continue;
                }            
                doc.add(args[i]);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (paths == null) {
            System.err.println("Paths file not given");
            System.err.println(USAGE);
            System.exit(1);
        }
        if (doc.isEmpty()) {
            System.err.println("Document source not given");
            System.err.println(USAGE);
            System.exit(1);
        }
        if (data == null) {
            System.out.println("Using " + paths + " to extract content from " + doc);
        } else {
            System.out.println("Substituting locations " + paths + " in " + doc + " with " + data);
        }
        try {
            BulkXpathExecutor bxe = new BulkXpathExecutor(paths);
            bxe.setOutputManager(om);
            bxe.setData(data);
            bxe.processDocuments(doc);
            ArrayList<String> errors = bxe.getOutputManager().getErrors();
            if ((errors != null) && (!errors.isEmpty())) {
                System.err.println("Non-fatal processing errors:");
                for (String s : errors) {
                    System.err.println(s);
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * @param paths
     * @throws Exception 
     */
    public BulkXpathExecutor(String paths) 
            throws Exception
    {
        @SuppressWarnings("UnusedAssignment")
        InputStream in = null;
        if (paths.contentEquals("-")) {
            in = System.in;
        } else {
            in = new FileInputStream(paths);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        @SuppressWarnings("UnusedAssignment")
        String line = null;
        XPathFactory xpf = XPathFactory.newInstance();
        while ((line = br.readLine()) != null) {
            if (line.contentEquals("."))
                break;
            if (line.contains("\t")) {
                String[] s = line.split("\t");
                XPath xp = xpf.newXPath();
                xp.setNamespaceContext(nhsdNS);
                XPathExpression exp = xp.compile(s[1]);
                DescribedXPath x = new DescribedXPath(s[1], exp);
                expressions.put(s[0], x);                
            } else {
                XPath xp = xpf.newXPath();
                xp.setNamespaceContext(nhsdNS);
                XPathExpression exp = xp.compile(line);
                DescribedXPath x = new DescribedXPath(line, exp);
                expressions.put(line, x);                
            }
        }
    }
    
    public void setOutputManager(OutputManager om) { outputManager = om; }

    public OutputManager getOutputManager() { return outputManager; }
    
    public void processDocuments(ArrayList<String> d)
            throws Exception
    {
        if (outputManager == null)
            outputManager = new OutputManager();
        
        for (String s : d) {
            outputManager.setCurrentFile(s);
            process(s);
        }
    }
      
    /**
     *
     * @param d
     * @throws Exception
     */
    public void setData(String d)
            throws Exception
    {
        if (d == null)
            return;
        
        substitutions = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(d));
        @SuppressWarnings("UnusedAssignment")
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] s = line.split("\t");
            ArrayList<String> list = new ArrayList<>();
            for (int i = 1; i < s.length; i++) {
                if (s[i].startsWith("$")) {
                    list.add(resolveFunction(s[i]));
                } else {
                    list.add(s[i]);
                }
            }
            substitutions.put(s[0], list);
        }
    }
    
    private String resolveFunction(String s) {
        
        if (s.startsWith("$TIME")) {
            return ISO8601TIME.format(new Date());
        }
        if (s.startsWith("$DATE")) {
            return ISO8601DATE.format(new Date());
        }
        if (s.startsWith("$UUID")) {
            return UUID.randomUUID().toString().toLowerCase();
        }
        // Reference to something else, which we'll resolve as we need it
        //
        return s;
    }
    
    private void process(String doc) 
            throws Exception
    {
        boolean streamOutput = true;
        
        @SuppressWarnings("UnusedAssignment")
        Document d = getDocument(doc);
        for(String s : expressions.keySet()) {
            DescribedXPath xp = expressions.get(s);
            XPathExpression exp = xp.getExpression();
            NodeList nl = (NodeList)exp.evaluate(d, XPathConstants.NODESET);
            if (substitutions == null) {
                StringBuilder sb = new StringBuilder(s);
                sb.append(System.getProperty("line.separator"));
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i);
                    String nsuri = n.getNamespaceURI();
                    if (nsuri != null) {
                        sb.append(nsuri);
                        sb.append(":");
                    }
                    if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                        sb.append("@");
                    }
                    sb.append(n.getNodeName());
                    sb.append("\t");
                    sb.append(n.getNodeValue());
                    sb.append(System.getProperty("line.separator"));
                }
                outputManager.output(sb.toString());
            } else {
                ArrayList<String> subs = substitutions.get(s);
                if (subs == null) {
                    continue;
                }
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i);
                    try {
                        String v = subs.get(i);
                        if (v.startsWith("xmlfragment:")) {
                            if (n.getNodeType() == Node.ELEMENT_NODE) {
                                Element elem = getElement(v.substring("xmlfragment:".length()));
                                elem = (Element)d.importNode(elem, true);
                                n.getParentNode().replaceChild(elem, n);
                            } else {
                                StringBuilder erep = new StringBuilder("WARNING: Ignoring substitution. Attempt to substitute XML fragment "); 
                                erep.append(v.substring("xmlfragment:".length()));
                                erep.append(" into non-element location ");
                                erep.append(xp.getXpath());
                                erep.append(": XML fragment substitutions can only be made into elements.");
                                outputManager.error(erep.toString());
                            }
                        } else {
                            if (v.startsWith("$")) {
                                String r = v.substring(1);
                                ArrayList<String> vs = substitutions.get(r);
                                if (vs != null) {
                                    v = vs.get(i);
                                    n.setNodeValue(v);
                                } else {
                                    StringBuilder erep = new StringBuilder("WARNING: Ignoring substitution. Label ");
                                    erep.append(s);
                                    erep.append(" references another: ");
                                    erep.append(v);
                                    erep.append(" which is not defined.");
                                    outputManager.error(erep.toString());
                                }
                            } else {
                                n.setNodeValue(v);
                            }                            
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        n.setNodeValue("");
                    }
                }
            }
        }
        if (substitutions != null) {
            outputManager.output(getStringFromDoc(d));
        }
    }
    
    private Element getElement(String s)
            throws Exception
    {
        Element elem = parse(s).getDocumentElement();
        return elem;
    }
    
    private String getStringFromDoc(Document doc)    {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        return lsSerializer.writeToString(doc);   
    }    

    private Document parse(String s)
            throws Exception
    {
        InputSource is = new InputSource(new StringReader(s));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xml = db.parse(is);
        return xml;        
    }
    
    private Document getDocument(String d)
            throws Exception
    {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("UnusedAssignment")
        InputStream in = null;
        if (d.contentEquals("-")) {
            in = System.in;
        } else {
            in = new FileInputStream(d);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        @SuppressWarnings("UnusedAssignment")
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return parse(sb.toString());
    }
}
