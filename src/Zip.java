import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip {
    final static int BUFFER_SIZE = 1024;

    public static void unzipArchive(String inputZipFilePath, String outputFilePath) throws IOException {
        String fileZip = inputZipFilePath;
        File destDir = new File(outputFilePath);
        byte[] buffer = new byte[BUFFER_SIZE];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void zipArchive(File inputPath, File outputPath) throws IOException {
        final int BUFFER = 1024;
        File[] files = inputPath.listFiles();
        if (files.length == 0)
            throw new IllegalArgumentException("No files in path " + inputPath.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(outputPath);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        for (File zipThis : files) {
            if (zipThis.isDirectory()) {
                zipOut.putNextEntry(new ZipEntry(zipThis.getName() + "/"));
                for(File zip2 : zipThis.listFiles()) {
                    addFile(zip2, zipOut, zipThis.getName());
                }
                continue;
            }
            addFile(zipThis, zipOut, "");
        }
        zipOut.close();
        fos.close();
    }

    public static void addFile(File zipThis, ZipOutputStream zipOut, String parentDirectoryName) throws IOException {
        final int BUFFER = 1024;
        FileInputStream fis = new FileInputStream(zipThis);
        if (!parentDirectoryName.isEmpty()) {
            ZipEntry zipEntry = new ZipEntry(parentDirectoryName + "/" + zipThis.getName());
            zipOut.putNextEntry(zipEntry);
        } else {
            ZipEntry zipEntry = new ZipEntry(zipThis.getName());
            zipOut.putNextEntry(zipEntry);
        }
        byte[] buffer = new byte[BUFFER];
        int length;
        while ((length = fis.read(buffer)) >= 0) {
            zipOut.write(buffer, 0, length);
        }
        fis.close();
    }
}
