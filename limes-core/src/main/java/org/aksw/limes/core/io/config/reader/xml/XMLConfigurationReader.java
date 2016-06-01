package org.aksw.limes.core.io.config.reader.xml;

import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.config.KBInfo;
import org.aksw.limes.core.io.config.reader.AConfigurationReader;
import org.aksw.limes.core.io.ls.LinkSpecification;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Mohamed Sherif <sherif@informatik.uni-leipzig.de>
 * @version Nov 12, 2015
 */
public class XMLConfigurationReader extends AConfigurationReader {
    private static final Logger logger = Logger.getLogger(XMLConfigurationReader.class);

    // Constants
    protected static final String FILE = "FILE";
    protected static final String GRANULARITY = "GRANULARITY";
    protected static final String TARGET = "TARGET";
    protected static final String LABEL = "LABEL";
    protected static final String RELATION = "RELATION";
    protected static final String THRESHOLD = "THRESHOLD";
    protected static final String REVIEW = "REVIEW";
    protected static final String ACCEPTANCE = "ACCEPTANCE";
    protected static final String EXECUTION = "EXECUTION";
    protected static final String OUTPUT = "OUTPUT";
    protected static final String TYPE = "TYPE";
    protected static final String VAR = "VAR";
    protected static final String ID = "ID";
    protected static final String SOURCE = "SOURCE";
    protected static final String PREFIX = "PREFIX";
    protected static final String PAGESIZE = "PAGESIZE";
    protected static final String ENDPOINT = "ENDPOINT";
    protected static final String GRAPH = "GRAPH";
    protected static final String RESTRICTION = "RESTRICTION";
    protected static final String PROPERTY = "PROPERTY";
    protected static final String AS = " AS ";
    protected static final String RENAME = " RENAME ";
    protected static final String METRIC = "METRIC";
    protected static final String NAMESPACE = "NAMESPACE";
    protected static final String REWRITER = "REWRITER";
    protected static final String PLANNER = "PLANNER";
    protected static final String ENGINE = "ENGINE";

    /**
     * Constructor
     */
    public XMLConfigurationReader(String fileNameOrUri) {
        super(System.getProperty("user.dir") + "/" + fileNameOrUri);
    }

    public static void processProperty(KBInfo kbinfo, String property) {
        String function = "", propertyLabel = "", propertyRename = "";
        //no preprocessing nor renaming

        if (!property.contains(RENAME) && !property.contains(AS)) {
            propertyLabel = property;
            propertyRename = property;
        } else if (!property.contains(RENAME) && property.contains(AS)) {
            propertyLabel = property.substring(0, property.indexOf(AS));
            propertyRename = propertyLabel;
            function = property.substring(property.indexOf(AS) + AS.length(), property.length());
        } else if (!property.contains(AS) && property.contains(RENAME)) {
            propertyLabel = property.substring(0, property.indexOf(RENAME));
            propertyRename = property.substring(property.indexOf(RENAME) + RENAME.length(), property.length());
            function = null;
        } //property contains both AS and RENAME, in that order
        else {
            propertyLabel = property.substring(0, property.indexOf(AS));
            function = property.substring(property.indexOf(AS) + AS.length(), property.indexOf(RENAME));
            propertyRename = property.substring(property.indexOf(RENAME) + RENAME.length(), property.length());
        }

        //now ensure that we have a map for the given label
        if (!kbinfo.getFunctions().containsKey(propertyLabel)) {
            kbinfo.getFunctions().put(propertyLabel, new HashMap<String, String>());
        }

        kbinfo.getFunctions().get(propertyLabel).put(propertyRename, function);

        //might be that the same label leads to two different propertydubs
        if (!kbinfo.getProperties().contains(propertyLabel)) {
            kbinfo.getProperties().add(propertyLabel);
        }


    }

    /**
     * Returns the content of a node
     *
     * @param node
     *         an item of the form <NODE> text </NODE>
     * @return The text between <NODE> and </NODE>
     */
    public static String getText(Node node) {

        // We need to retrieve the text from elements, entity
        // references, CDATA sections, and text nodes; but not
        // comments or processing instructions
        int type = node.getNodeType();
        if (type == Node.COMMENT_NODE
                || type == Node.PROCESSING_INSTRUCTION_NODE) {
            return "";
        }

        StringBuffer text = new StringBuffer();

        String value = node.getNodeValue();
        if (value != null) {
            text.append(value);
        }
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                text.append(getText(child));
            }
        }
        return text.toString();
    }

    /**
     * Returns a filled out configuration object if the input complies
     * to the LIMES DTD and contains everything needed.
     * NB: The path to the DTD must be specified in the input file
     *
     * @param input
     *         The input XML file
     * @return filled out configuration if parsing was successful, else false
     */
    @Override
    public Configuration read() {
        try {
            File f = new File(fileNameOrUri);
            InputStream input = new FileInputStream(f);
            return validateAndRead(input, fileNameOrUri);
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
            logger.warn("Some values were not set. Crossing my fingers and using defaults.");
        }
        return configuration;
    }

    public void afterPropertiesSet(Configuration configuration) {
        configuration.getSourceInfo().afterPropertiesSet();
        configuration.getTargetInfo().afterPropertiesSet();

        configuration.getSourceInfo().setPrefixes(configuration.getPrefixes());
        configuration.getTargetInfo().setPrefixes(configuration.getPrefixes());
    }

    public void processKBDescription(String kb, NodeList children) {
        KBInfo kbinfo;
        if (kb.equalsIgnoreCase("source")) {
            kbinfo = configuration.getSourceInfo();
        } else {
            kbinfo = configuration.getTargetInfo();
        }

        String property;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(ID)) {
                kbinfo.setId(getText(child));
            } else if (child.getNodeName().equals(ENDPOINT)) {
                kbinfo.setEndpoint(getText(child));
            } else if (child.getNodeName().equals(GRAPH)) {
                kbinfo.setGraph(getText(child));
            } else if (child.getNodeName().equals(RESTRICTION)) {
                String restriction = getText(child).trim();
                if (restriction.endsWith(".")) {
                    restriction = restriction.substring(0, restriction.length() - 1);
                }
                kbinfo.addRestriction(restriction);
            } else if (child.getNodeName().equals(PROPERTY)) {
                property = getText(child);
                processProperty(kbinfo, property);
            } else if (child.getNodeName().equals(PAGESIZE)) {
                kbinfo.setPageSize(Integer.parseInt(getText(child)));
            } else if (child.getNodeName().equals(VAR)) {
                kbinfo.setVar(getText(child));
            } else if (child.getNodeName().equals(TYPE)) {
                kbinfo.setType(getText(child));
            }
        }
        kbinfo.setPrefixes(configuration.getPrefixes());
    }

    /**
     * Returns true if the input complies to the LIMES DTD and contains
     * everything needed. NB: The path to the DTD must be specified in the input
     * file
     *
     * @param input
     *         The input XML file as Stream
     * @return true if parsing was successful, else false
     */
    public Configuration validateAndRead(InputStream input, String filePath) {
        DtdChecker dtdChecker = new DtdChecker();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //make sure document is valid
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(dtdChecker);

            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    //                    System.out.println(systemId);
                    if (systemId.contains("limes.dtd")) {
                        String dtd = System.getProperty("user.dir") + "/resources/limes.dtd";
                        return new InputSource(dtd);
                    } else {
                        return null;
                    }
                }
            });
            Document xmlDocument = builder.parse(input);
            if (dtdChecker.valid) {
                //0. Prefixes
                NodeList list = xmlDocument.getElementsByTagName(PREFIX);
                NodeList children;
                String namespace = "", label = "";
                for (int i = 0; i < list.getLength(); i++) {
                    children = list.item(i).getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.getNodeName().equals(NAMESPACE)) {
                            namespace = getText(child);
                        } else if (child.getNodeName().equals(LABEL)) {
                            label = getText(child);
                        }
                    }
                    logger.info(label);
                    configuration.addPrefixes(label, namespace);
                }
                //1. Source information
                list = xmlDocument.getElementsByTagName(SOURCE);
                children = list.item(0).getChildNodes();
                processKBDescription(SOURCE, children);
                //2. Target information
                list = xmlDocument.getElementsByTagName(TARGET);
                children = list.item(0).getChildNodes();
                processKBDescription(TARGET, children);
                //3.METRIC
                list = xmlDocument.getElementsByTagName(METRIC);
                configuration.setMetricExpression(getText(list.item(0)));

                //4. ACCEPTANCE file and conditions
                list = xmlDocument.getElementsByTagName(ACCEPTANCE);
                children = list.item(0).getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeName().equals(THRESHOLD)) {
                        configuration.setAcceptanceThreshold(Double.parseDouble(getText(child)));
                    } else if (child.getNodeName().equals(FILE)) {
                        String file = getText(child);
                        configuration.setAcceptanceFile(file);
                    } else if (child.getNodeName().equals(RELATION)) {
                        configuration.setAcceptanceRelation(getText(child));
                    }
                }

                //5. VERIFICATION file and conditions
                list = xmlDocument.getElementsByTagName(REVIEW);
                children = list.item(0).getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeName().equals(THRESHOLD)) {
                        configuration.setVerificationThreshold(Double.parseDouble(getText(child)));
                    } else if (child.getNodeName().equals(FILE)) {
                        String file = getText(child);
                        configuration.setVerificationFile(file);
                    } else if (child.getNodeName().equals(RELATION)) {
                        configuration.setVerificationRelation(getText(child));
                    }
                }

                //6. EXECUTION 
                //                if (list.getLength() > 0) {
                //                    list = xmlDocument.getElementsByTagName(EXECUTION);
                //                    children = list.item(0).getChildNodes();
                //                    configuration.setExecutionPlan(getText(list.item(0)));
                //                }
                if (list.getLength() > 0) {
                    list = xmlDocument.getElementsByTagName(EXECUTION);
                    for (int i = 0; i < list.getLength(); i++) {
                        children = list.item(i).getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node child = children.item(j);
                            if (child.getNodeName().equals(REWRITER)) {
                                configuration.setExecutionRewriter(getText(child));
                            } else if (child.getNodeName().equals(PLANNER)) {
                                configuration.setExecutionPlanner(getText(child));
                            }else if (child.getNodeName().equals(ENGINE)) {
                                configuration.setExecutionEngine(getText(child));
                            }
                        }
                        logger.info(label);
                        configuration.addPrefixes(label, namespace);
                    }
                }

                //7. TILING if necessary
                list = xmlDocument.getElementsByTagName(GRANULARITY);
                if (list.getLength() > 0) {
                    children = list.item(0).getChildNodes();
                    configuration.setGranularity(Integer.parseInt(getText(list.item(0))));
                } else {
                }

                //8. OUTPUT format
                list = xmlDocument.getElementsByTagName(OUTPUT);
                if (list.getLength() > 0) {
                    children = list.item(0).getChildNodes();
                    configuration.setOutputFormat(getText(list.item(0)));
                } 
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
            logger.warn("Some values were not set. Crossing my fingers and using defaults.");
        }
        return configuration;
    }

    public void modifyMetricExpression(LinkSpecification spec) {
        for (LinkSpecification atomicSpec : spec.getAllLeaves()) {
            String m = atomicSpec.getFilterExpression();
            logger.info(m);
            Pattern p = Pattern.compile(Pattern.quote(m) + "\\|\\d*\\.\\d+");
            String metricExpr = configuration.getMetricExpression();
            Matcher mac = p.matcher(metricExpr);
            if (mac.find()) {
                int start = mac.start();
                int end = mac.end();
                String subStr = metricExpr.substring(start, end);
                String[] arr = subStr.split("\\|");
                configuration.setMetricExpression(metricExpr.replace(subStr, arr[0] + "|" + Double.toString(atomicSpec.getThreshold())));
            }
        }
    }
}
