// Align Workflow elements to a nice looking grid

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author fabian.schmidberger
 *  Class can be called from command line as such:
 *  java WorkflowEnhancer <path-to-zip-file> [tolerance]
 *  java WorkflowEnhancer "C:\Users\fabian.schmidberger\Downloads\workflow.zip" 10
 *  The tolerance parameter is optional. If no tolerance is given, default tolerance 10 is used.
 */
@SuppressWarnings("ALL")
public class WorkflowEnhancer {
    private static void copyZip(String originalPath) throws IOException {
        File originalFile = new File(originalPath);
        String newPath = originalPath.replace(".zip", " - original.zip");

        File newFile = new File(newPath);

        try {
            if (newFile.exists()) {
                deleteDirectory(newFile);
            }

            Files.copy(originalFile.toPath(), newFile.toPath());
            System.out.println("A backup of the original Workflow Template got created at: " + newFile.toPath());
        } catch (Exception e) {
            System.out.println("Error while backing up the zip file");
        }
    }

    /***
     * To delete a directory recursively (with its content)
     * @param directory The directory to be deleted
     * @return
     */
    private static boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        return (directory.delete());
    }

    /***
     * Usage: java main <PathToWorkflowZip> <tolerance-level>
     *     e.g. java main "C:\Users\fabian.schmidberger\Downloads\7.zip" 10
     * @param args
     */
    public static void main(String[] args) {
        String inputZip = args[0];
        int toleranceLevel = 10;

        String outputPath = System.getProperty("java.io.tmpdir") + "Workflow-Enhancer" + File.separator + "workflow-output";
        final String OUTPUT_FOLDER = System.getProperty("java.io.tmpdir") + File.separator + "Workflow-Enhancer";
        final String OUTPUT_PATH = OUTPUT_FOLDER + File.separator + "workflow-output";
        try {
            if (args.length == 2) {
                toleranceLevel = Integer.parseInt(args[1]);
            } else if (args.length != 1) {
                throw new IOException("The command needs to have either 1 or 2 arguments");
            }

            Zip.unzipArchive(inputZip, outputPath);
            File outputFolder = new File(outputPath);
            File[] fileList = outputFolder.listFiles();
            for (File file : fileList) {
                if (file.getName().contains("TAG-NmLoader-1")) {
                    throw new IOException("The utility only supports modifying a single Workflow Template. The zip file contains more than one Workflow Template.");
                }
            }
            DomParser.modifyXml(OUTPUT_PATH + File.separator + "TAG-NmLoader-0.xml", toleranceLevel);

            // make backup of original workflow zip
            copyZip(inputZip);

            // delete original zip file
            deleteDirectory(new File(inputZip));

            Zip.zipArchive(new File(OUTPUT_PATH), new File(inputZip));


        } catch (IOException | ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            // delete temp directory
            //FIXME delete temp directory even if program fails/crashes at one point
            deleteDirectory(new File(OUTPUT_FOLDER));
        }
    }
}