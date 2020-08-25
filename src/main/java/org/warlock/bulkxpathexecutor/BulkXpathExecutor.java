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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.warlock.util.CfHNamespaceContext;
import org.xml.sax.InputSource;

/**
 * Usage: java -jar BulkXpathExecutor.jar -p pathsfile [ -r datafile ]* [ -m ] [
 * -M ] [ -f ] [ -t ] [ -o outputfile ] [ -e errorfile ] [ -x extension ] [ -X
 * extension ] [ documentfile+ | - ]
 *
 * Takes well formed xml input files and bulk modifies and outputs them as well
 * formed modified xml files according to data in paths and data files.
 *
 * @param -p &lt;paths file&gt; tab separated file containing pairs of
 * identifiers and xpaths. Comments start with #. Associates an identifier with
 * an xpath
 * @param -r &lt;data file&gt; optional (0..n) tab separated file containing
 * pairs of identifiers and data values to be assigned to those identifiers.
 * Comments start with #. Associates an identifier with a value to be applied in
 * output file.
 * @param -m optional set in memory output Outputs are written to lists of
 * string (for using this jar as a library)
 * @param -M optional set in memory error Errors are written to lists of string
 * (for using this jar as a library)
 * @param -f optional prepend filename to error
 * @param -t optional include a timestamp with the error
 * @param -o optional &lt;output file&gt; path to file to which modified xml
 * file is to be output
 * @param -e optional &lt;error file&gt; path to file to which modification
 * errors are to be output
 * @param -x &lt;extension&gt; optional file extension to be appended to output
 * files.
 * @param -X &lt;extension&gt; optional file extension to be appended to error
 * files
 * @param &lt;document file&gt; 1 or more paths to well formed xml input files
 * or stdin
 *
 * @author Damian Murphy
 */
public class BulkXpathExecutor {

    private static final String USAGE = "Usage: java -jar BulkXpathExecutor.jar -p pathsfile [ -r datafile ]* [ -m ] [ -M ] [ -f ] [ -t ] [ -o outputfile ] [ -e errorfile ] [ -x extension ] [ -X extension ] [ documentfile | - ]";
    private HashMap<String, DescribedXPath> expressions = new HashMap<>();
    private HashMap<String, ArrayList<String>> substitutions = null;
    private NamespaceContext nhsdNS = CfHNamespaceContext.getXMLNamespaceContext();
    private static final SimpleDateFormat ISO8601TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat ISO8601DATE = new SimpleDateFormat("yyyy-MM-dd");

    private OutputManager outputManager = null;

    // Reserved words
    private static final String RESERVED_WORD_UUID = "$UUID";
    private static final String RESERVED_WORD_TODAY = "$TODAY";
    private static final String RESERVED_WORD_DATE = "$DATE";
    private static final String RESERVED_WORD_TIME = "$TIME";
    private static final String RESERVED_WORD_DELETE = "$DELETE";
    private static final String RESERVED_WORD_VALUEDATEOFFSET = "$VALUEDATEOFFSET";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String paths = null;
        ArrayList<String> datafiles = new ArrayList<>();
        ArrayList<String> doc = new ArrayList<>();
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
                    datafiles.add(args[i]);
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
        } catch (Exception e) {
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
        if (datafiles == null || datafiles.isEmpty()) {
            System.out.println("Using " + paths + " to extract content from " + doc);
        } else {
            System.out.println("Substituting locations " + paths + " in " + doc + " with " + String.join(",", datafiles));
        }
        try {
            BulkXpathExecutor bxe = new BulkXpathExecutor(paths);
            bxe.setOutputManager(om);
            bxe.setData(datafiles.toArray(new String[datafiles.size()]));
            bxe.processDocuments(doc);
            ArrayList<String> errors = bxe.getOutputManager().getErrors();
            if ((errors != null) && (!errors.isEmpty())) {
                System.err.println("Non-fatal processing errors:");
                for (String s : errors) {
                    System.err.println(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * reads paths file and populates expressions
     *
     * @param paths String path to paths file
     * @throws Exception
     */
    private BulkXpathExecutor(String paths)
            throws Exception {
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
            // email style terminator for use with streams
            if (line.contentEquals(".")) {
                break;
            }
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
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

    private void setOutputManager(OutputManager om) {
        outputManager = om;
    }

    private OutputManager getOutputManager() {
        return outputManager;
    }

    /**
     * iterates through documents and processes them
     *
     * @param documents ArrayList&lt;String&gt; paths to documents files
     * @throws Exception
     */
    private void processDocuments(ArrayList<String> documents)
            throws Exception {
        if (outputManager == null) {
            outputManager = new OutputManager();
        }

        for (String document : documents) {
            outputManager.setCurrentFile(document);
            process(document);
        }
    }

    /**
     * iterates through data files and populates substitutions
     *
     * @param datafiles String[] of data files
     * @throws Exception
     */
    private void setData(String[] datafiles)
            throws Exception {

        if (datafiles == null || datafiles.length == 0) {
            return;
        }

        substitutions = new HashMap<>();
        for (String datafile : datafiles) {
            BufferedReader br = new BufferedReader(new FileReader(datafile));
            @SuppressWarnings("UnusedAssignment")
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                String[] s = line.split("\t");
                ArrayList<String> list = new ArrayList<>();
                for (int i = 1; i < s.length; i++) {
                    if (s[i].startsWith("$")) {
                        list.add(resolveFunction(s[i]).trim());
                    } else {
                        list.add(s[i]);
                    }
                }
                substitutions.put(s[0], list);
            }
        }
    }

    /**
     * evaluates reserved words
     *
     * @param s name of reserved word
     * @return evaluates substitution
     */
    private String resolveFunction(String s) {

        // TODO: Add ISO8601 duration offsets to $TIME and $DATE
        if (s.contentEquals(RESERVED_WORD_TIME)) {
            return ISO8601TIME.format(new Date());
        }
        if (s.contentEquals(RESERVED_WORD_DATE)) {
            return ISO8601DATE.format(new Date());
        }
        // $TODAY is a timestamp starting at 00:00:00 today.
        if (s.contentEquals(RESERVED_WORD_TODAY)) {
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return ISO8601TIME.format(cal.getTime());
        }

        if (s.startsWith(RESERVED_WORD_TIME)) {
            return resolveOffset(s, false);
        }
        if (s.startsWith(RESERVED_WORD_DATE)) {
            return resolveOffset(s, true);
        }
        if (s.startsWith(RESERVED_WORD_TODAY)) {
            return resolveOffset(s, false);
        }
        if (s.startsWith(RESERVED_WORD_UUID)) {
            return UUID.randomUUID().toString().toLowerCase();
        }
        // Reference to something else, which we'll resolve as we need it
        //
        return s;
    }

    /**
     *
     * @param spec String specification of date or time eg $TODAYPT9H
     * @param dt date or time boolean true means date
     * @return resolved time/date string
     */
    private String resolveOffset(String spec, boolean dt) {

        String durationStr = null;
        Instant now = null;
        if (spec.startsWith(RESERVED_WORD_TODAY)) {
            durationStr = spec.substring(RESERVED_WORD_TODAY.length()).trim();
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
            LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
            // TODO I don't understand why this appears to work during BST
            now = todayMidnight.toInstant(ZoneOffset.UTC);
        } else {
            durationStr = spec.substring(RESERVED_WORD_TIME.length()).trim();
            now = Instant.now();
        }

        Duration offset = Duration.parse(durationStr);

        @SuppressWarnings("UnusedAssignment")
        Instant then = null;
        then = now.plus(offset);
        // Should be ISO-8601 anyway
        if (!dt) {
            return then.toString().substring(0, 19);
        }
        String t = then.toString();
        return t.substring(0, t.indexOf("T"));
    }

    /**
     * processes input document
     *
     * @param doc String containing path to xml document
     * @throws Exception
     */
    private void process(String doc)
            throws Exception {

        @SuppressWarnings("UnusedAssignment")
        Document d = getDocument(doc);
        HashMap<String, NodeList> nodelists = new HashMap<>();
        // first pass constructs and caches all the nodelists, the second pass makes the substitutions.
        // This avoids conflicts around modifying a dom that you are still querying
        for (int pass = 0; pass < 2; pass++) {
            for (String expression : expressions.keySet()) {
                DescribedXPath xp = expressions.get(expression);
                if (pass == 0) {
                    XPathExpression exp = xp.getExpression();
                    NodeList nl = (NodeList) exp.evaluate(d, XPathConstants.NODESET);
                    nodelists.put(expression, nl);
                } else {
                    // no substitutions ie no data file so generate datafile like output
                    NodeList nl = nodelists.get(expression);
                    if (substitutions == null) {
                        StringBuilder sb = new StringBuilder(expression);
                        //sb.append(System.getProperty("line.separator"));
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node n = nl.item(i);
                            String nsuri = n.getNamespaceURI();
                            if (nsuri != null) {
                                sb.append(nsuri);
                                sb.append(":");
                            }
                            if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                                //sb.append("@");
                            }
                            //sb.append(n.getNodeName());
                            sb.append("\t");
                            sb.append(n.getNodeValue());
                            // sb.append(System.getProperty("line.separator"));
                        }
                        outputManager.output(sb.toString());
                    } else {
                        // substitutions driven by datafile
                        ArrayList<String> subs = substitutions.get(expression);
                        if (subs == null || subs.isEmpty()) {
                            continue;
                        }
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node n = nl.item(i);

                            String v = null;
                            // There can be > 1 substitutions to match multiple matches but if there aren't just use the first one
                            // and if there isnt one at all set an empty string. This is not fhir valid xml but that will be trapped
                            // by subsequent fhir validation
                            try {
                                v = subs.get(i);
                            } catch (IndexOutOfBoundsException e) {
                                v = subs.get(0);
                                if (v == null || v.trim().isEmpty()) {
                                    n.setNodeValue("");
                                    continue;
                                }
                            }
                            if (v.startsWith("xmlfragment:")) {
                                if (n.getNodeType() == Node.ELEMENT_NODE) {
                                    Element elem = getElement(v.substring("xmlfragment:".length()));
                                    elem = (Element) d.importNode(elem, true);
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
                                    if (v.equals(RESERVED_WORD_DELETE)) {
                                        if (n.getParentNode() != null) {
                                            // System.err.println("Deleting " + n.getLocalName() + " "+ n.getAttributes().getNamedItem("value"));
                                            // n needs to be an element not an attribute. Attributes don't have parents
                                            n.getParentNode().removeChild(n);
                                        } else {
                                            outputManager.error("Failed to delete " + n.getLocalName() + " no parent ");
                                        }
                                        continue;
                                    } else if (v.startsWith(RESERVED_WORD_VALUEDATEOFFSET)) {
                                        // applies a date offset to the date part of the source document field. the string may be a date only or date time field with trailing chars
                                        // but the duration must be days only with no hours, mins or secs
                                        String dateStr = n.getNodeValue();
                                        // see https://www.hl7.org/fhir/datatypes.html#dateTime
                                        if (dateStr.matches("^([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?$")) {
                                            final int DATE_LENGTH = 10;
                                            String durationsStr = v.substring(RESERVED_WORD_VALUEDATEOFFSET.length()).trim();
                                            Duration duration = Duration.parse(durationsStr);
                                            LocalDate localDate = LocalDate.parse(dateStr.substring(0, DATE_LENGTH));
                                            localDate = localDate.plus(duration.toDays(), ChronoUnit.DAYS);
                                            dateStr = dateStr.replaceFirst("^.{" + DATE_LENGTH + "}", localDate.toString().substring(0, DATE_LENGTH));
                                            n.setNodeValue(dateStr);
                                        } else {
                                            outputManager.error("Failed to parse malformed date string " + dateStr + " at node " + n.getLocalName());
                                        }
                                        continue;
                                    }
                                    String r = v.substring(1);
                                    ArrayList<String> vs = substitutions.get(r);
                                    if (vs != null) {
                                        try {
                                            v = vs.get(i);
                                        } catch (IndexOutOfBoundsException e) {
                                            v = vs.get(0);
                                        }
                                        if (v != null && !v.trim().isEmpty()) {
                                            n.setNodeValue(v);
                                        } else {
                                            n.setNodeValue("");
                                        }

                                    } else {
                                        StringBuilder erep = new StringBuilder("WARNING: Ignoring substitution. Label ");
                                        erep.append(expression);
                                        erep.append(" references another: ");
                                        erep.append(v);
                                        erep.append(" which is not defined.");
                                        outputManager.error(erep.toString());
                                    }
                                } else {
                                    n.setNodeValue(v);
                                }
                            } // if xml fragment
                        } // for nodelist
                    } // there exist substitutions
                } // second pass
            } // for expression
        } // for pass
        if (substitutions != null) {
            outputManager.output(getStringFromDoc(d));
        }
    }

    /**
     *
     * @param s xpath to element
     * @return populated Element object
     * @throws Exception
     */
    private Element getElement(String s)
            throws Exception {
        Element elem = parse(s).getDocumentElement();
        return elem;
    }

    /**
     * serialises a Document object
     *
     * @param doc Document object
     * @return Serialised String
     */
    private String getStringFromDoc(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        DOMStringList x = lsSerializer.getDomConfig().getParameterNames();
        return lsSerializer.writeToString(doc);
    }

    /**
     * parse a string containing xml into a Document object
     *
     * @param s
     * @return Document object
     * @throws Exception
     */
    private Document parse(String s)
            throws Exception {
        InputSource is = new InputSource(new StringReader(s));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xml = db.parse(is);
        return xml;
    }

    /**
     *
     * @param d String containing path to document file
     * @return populated xml Document object
     * @throws Exception
     */
    private Document getDocument(String d)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("UnusedAssignment")
        InputStream in = null;
        if (d.contentEquals("-")) {
            in = System.in;
        } else {
            in = new FileInputStream(d);
        }

        // changed from bufferedread terminated by n/l since we have some attributes with embedded newlines
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        in.close();
        return parse(result.toString("UTF-8"));
    }
}
