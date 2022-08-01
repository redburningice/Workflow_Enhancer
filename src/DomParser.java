import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DomParser {
    //TODO update javadoc of all methods

    static final String UNIQUE_NODE_ID = "csvuniqueId";
    static final String LOAD_EXTERNAL_DTD_SETTING = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    static final String DTD_SEARCH_EVERYTHING = "*";
    static final String X_COORDINATE = "csvxCoordinate";
    static final String Y_COORDINATE = "csvyCoordinate";
    ArrayList<WorkflowElement> workflowElements;

    /***
     * Parses a Workflow XML
     * @param WorkflowPath The Path to the XML containing a Workflow Template
     * @param tolerance Defines how many pixels should be used to smoothen an axis
     */
    public static void modifyXml(String WorkflowPath, int tolerance) throws ParserConfigurationException {
        LinkedHashMap<String, WorkflowElement> workflowMap = parseXml(WorkflowPath, tolerance);
        overwriteXml(workflowMap, WorkflowPath);
    }

    private static LinkedHashMap<String, WorkflowElement> parseXml(String WorkflowPath, int tolerance) {
        LinkedHashMap<String, WorkflowElement> output = new LinkedHashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // Disables DTD validating. Necessary to read xml file.
            factory.setFeature(LOAD_EXTERNAL_DTD_SETTING, false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document workflowXml = builder.parse(WorkflowPath);
            NodeList idNodeList = workflowXml.getElementsByTagName(UNIQUE_NODE_ID);
            NodeList childNodeList = null;
            LinkedHashMap<String, Integer> xCoordinatesList = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> yCoordinatesList = new LinkedHashMap<>();

            // get x and y Coordinates and fill the appropriate CoordinatesList
            for (int i = 0; i < idNodeList.getLength(); i++) {
                Node singleNode = idNodeList.item(i);
                String ID = singleNode.getTextContent();
                Node parentNode = singleNode.getParentNode();
                childNodeList = parentNode.getChildNodes();
                extractChildNodes(ID, childNodeList, xCoordinatesList, yCoordinatesList);
            }
            LinkedHashMap<String, Integer> xCoordinates = modifyCoordinates(xCoordinatesList, tolerance);
            LinkedHashMap<String, Integer> yCoordinates = modifyCoordinates(yCoordinatesList, tolerance);
            output = createWorkflowElements(xCoordinates, yCoordinates);
//
//            for (int i = 0; i < idNodeList.getLength(); i++) {
//                for (Map.Entry<String, WorkflowElement> entry : output.entrySet()) {
//                    Node singleNode = idNodeList.item(i);
//                    String ID = singleNode.getTextContent();
//                    if (ID.equals(entry.getKey())) {
//                        Node parentNode = singleNode.getParentNode();
//                        childNodeList = parentNode.getChildNodes();
//                        changeChildNodeValues(ID, childNodeList, entry.getValue());
//                    }
//
//                }
//            }

        } catch (ParserConfigurationException e) { //FIXME JL: Better throw the exceptions and handle in the calling method? In case of exception, are we even getting a valid result?
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return output;
    }

    private static void overwriteXml(LinkedHashMap<String, WorkflowElement> workflowMap, String WorkflowPath) throws ParserConfigurationException {
        final String FILENAME = WorkflowPath;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try (InputStream is = new FileInputStream(FILENAME)) {

            // Disables DTD validating. Necessary to read xml file.
            factory.setFeature(LOAD_EXTERNAL_DTD_SETTING, false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            NodeList nodelist = doc.getElementsByTagName(UNIQUE_NODE_ID);
            for (int i = 0; i < nodelist.getLength(); i++) {
                for (Map.Entry<String, WorkflowElement> entry : workflowMap.entrySet()) {
                    String ID = nodelist.item(i).getTextContent();
                    NodeList childNodeList = nodelist.item(i).getParentNode().getChildNodes();
                    WorkflowElement workflowElement = entry.getValue();
                    if (ID.equals(workflowElement.getCsvuniqueId())) {
                        changeChildNodeValues(ID, childNodeList, workflowElement);
                    }
                }
            }

            try (FileOutputStream output = new FileOutputStream(WorkflowPath)) {
                writeXml(doc, output);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    /***
     * Extracts Child Nodes from a list of Parent Nodes
     * @param ID unique ID for a Workflow Element
     * @param childNodeList list of nodes to extract child nodes from
     * @param xCoordinatesList List with <ID, xCoordinate> value pairs
     * @param yCoordinatesList List with <ID, yCoordinate> value pairs
     */
    private static void extractChildNodes(String ID, NodeList childNodeList, LinkedHashMap<String, Integer> xCoordinatesList, LinkedHashMap<String, Integer> yCoordinatesList) {
        for (int childNodeEntry = 0; childNodeEntry < childNodeList.getLength(); childNodeEntry++) {
            switch (childNodeList.item(childNodeEntry).getNodeName()) {
                case X_COORDINATE:
                    int xCoordinate = Integer.parseInt(childNodeList.item(childNodeEntry).getTextContent());
                    xCoordinatesList.put(ID, xCoordinate);
                    break;
                case Y_COORDINATE:
                    int yCoordinate = Integer.parseInt(childNodeList.item(childNodeEntry).getTextContent());
                    yCoordinatesList.put(ID, yCoordinate);
                    break;
                default:
                    break;
            }
        }
    }

    private static LinkedHashMap<String, Integer> modifyCoordinates(LinkedHashMap<String, Integer> coordinatesList, int tolerance) {
        coordinatesList = sort(coordinatesList);
        ArrayList<Group> group = group(coordinatesList, tolerance);
        equalize(group);
        return mergeLinkedHashMaps(group);
    }

    private static LinkedHashMap<String, WorkflowElement> createWorkflowElements(LinkedHashMap<String, Integer> xCoordinates, LinkedHashMap<String, Integer> yCoordinates) {
        LinkedHashMap<String, WorkflowElement> output = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entryX : xCoordinates.entrySet()) {
            for (Map.Entry<String, Integer> entryY : yCoordinates.entrySet()) {
                if (entryX.getKey().equals(entryY.getKey()))
                    output.put(entryX.getKey(), new WorkflowElement(entryX.getKey(), entryX.getValue(), entryY.getValue()));
            }
        }
        return output;
    }

    private static void changeChildNodeValues(String ID, NodeList childNodeList, WorkflowElement workflowElement) {
        for (int i = 0; i < childNodeList.getLength(); i++) {
            switch (childNodeList.item(i).getNodeName()) {
                case X_COORDINATE:
                    childNodeList.item(i).setTextContent(Integer.toString(workflowElement.getPoint().getxCoordinate()));
                    break;
                case Y_COORDINATE:
                    childNodeList.item(i).setTextContent(Integer.toString(workflowElement.getPoint().getyCoordinate()));
                    break;
                default:
                    break;
            }
        }
    }

    private static void writeXml(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        //add doctype tag
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        DOMImplementation domImpl = doc.getImplementation();
        DocumentType doctype = domImpl.createDocumentType("doctype","","standard11_1.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }

    /**
     * sorts a given LinkedHashMap by its value
     *
     * @param linkedHashMap
     *
     * @return
     */
    private static LinkedHashMap<String, Integer> sort(LinkedHashMap<String, Integer> linkedHashMap) {
        return linkedHashMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, LinkedHashMap::new));
    }

    /***
     *
     * @param coordinatesList
     * @param tolerance
     * @return
     */
    private static ArrayList<Group> group(LinkedHashMap<String, Integer> coordinatesList, int tolerance) {
        ArrayList<Group> listOfGroups = new ArrayList<>();
        Map.Entry<String, Integer> prev = null;
        int index = 0;
        int counter = 0;
        for (Map.Entry<String, Integer> curr : coordinatesList.entrySet()) {
            if (counter == 0) {
                // initialize first group
                listOfGroups.add(new Group(index, curr));
            } else if (curr.getValue() - prev.getValue() <= tolerance) {
                //in group
                listOfGroups.get(index).addGroupMember(curr);
            } else {
                // not in group
                index++;
                listOfGroups.add(new Group(index, curr));
            }
            prev = curr;
            counter++;
        }
        return listOfGroups;
    }

    /***
     *
     * @param listOfGroups
     */
    private static void equalize(ArrayList<Group> listOfGroups) {
        LinkedHashMap<String, Integer> workflowElements = null;
        for (Group entry : listOfGroups) {
            entry.setAvgCoordinate();
            entry.equalizeCoordinates();
        }
    }

    private static LinkedHashMap<String, Integer> mergeLinkedHashMaps(ArrayList<Group> listOfGroups) {
        LinkedHashMap<String, Integer> output = new LinkedHashMap<>();
        for (Group group : listOfGroups) {
            output.putAll(group.groupMembers);
        }
        return output;
    }

    /***
     * Prints a given LinkedHashMap to the console. Only for debugging purposes.
     * @param linkedHashMap
     */
    private static void printMap(LinkedHashMap<String, Integer> linkedHashMap) {
        for (Map.Entry<String, Integer> entry : linkedHashMap.entrySet()) {
            System.out.println(entry.getKey() + " | " + entry.getValue());
        }
    }
}