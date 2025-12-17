package es.github.TaigaSeven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue; 

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

//DL - Plugin para ejecutar tsDetect, enviandole como input fichero csv con las clases correspondientes, y producir la salida tanto en terminal como en formato csv.

//Ejecutar en fase de test
@Mojo(name="detect", defaultPhase = LifecyclePhase.TEST)
public class TestSmellMojo extends AbstractMojo{

    @Parameter(defaultValue="${project}", readonly=true)
    private MavenProject project;

    @Parameter(defaultValue="${project.basedir}/src/test/java", property="testsmells.testSourceDir")
    private File testSourceDirectory;

    @Parameter(defaultValue="${project.basedir}/src/main/java", property="testsmells.sourceDir")
    private File sourceDirectory;

    //link de tsDetect github version 2.2
    private static final String TSDETECT_JAR_URL = "https://github.com/TestSmells/TestSmellDetector/releases/download/v2.2/TestSmellDetector.jar";

    @Override
    public void execute() throws MojoExecutionException{
        getLog().info("------------------------------------------------------------------------");
        getLog().info("Test Smells Detection Plugin (tsDetect)");
        getLog().info("------------------------------------------------------------------------");

        //Mapear todos los archivos
        Map<String,String> testMainFilesMap = findTestAndMainFiles();

        if(testMainFilesMap.isEmpty()){
            getLog().warn("No files found in "+ sourceDirectory + " and " + testSourceDirectory.getAbsolutePath());
        }else{
            //Generar un archivo csv con información de archivos
            File csvInputFile = generateCsvInput(testMainFilesMap);

            //Ejecturar tsDetect
            File resultFile = executeTestSmellDetector(csvInputFile);

            //Mostrar el resultado en el terminal
            displayResults(resultFile);
        }
        
    }

    //Localizar los archivos de test y correspondientes main
    private Map<String,String> findTestAndMainFiles(){
        Map<String, String> testMainFilesMap = new LinkedHashMap<>();

        //Localizar todos los archivos de main
        Map<String,String> mainFilesMap = new HashMap<>();
        if(sourceDirectory.exists()){
            Queue<File> mainFilesQueue = new LinkedList<>();
            mainFilesQueue.add(sourceDirectory);

            while (!mainFilesQueue.isEmpty()) {
                File[] filesList = mainFilesQueue.poll().listFiles();
                if(filesList == null) continue;

                for (File f: filesList){
                    if(f.isDirectory()) mainFilesQueue.add(f);
                    else if (f.getName().endsWith(".java")) mainFilesMap.put(f.getName(), f.getAbsolutePath());
                }
            }
        }

        //Localizar todos los archivos de test y enparejar con su main correspondiente
        if(testSourceDirectory.exists()){
            Queue<File> testFilesQueue = new LinkedList<>();
            testFilesQueue.add(testSourceDirectory);

            while (!testFilesQueue.isEmpty()){
                File[] filesList = testFilesQueue.poll().listFiles();
                if (filesList == null) continue;

                for(File f : filesList){
                    if(f.isDirectory()) {
                        testFilesQueue.add(f);
                    } else if (f.getName().toLowerCase().endsWith("test.java")){
                        String testFilePath = f.getAbsolutePath();

                        String mainFileName = f.getName()
                            .replaceAll("(?i)tests?\\.java$",".java");

                        String mainFilePath = mainFilesMap.get(mainFileName);
                        testMainFilesMap.put(testFilePath, mainFilePath);
                    }
                }
            }
        }
        
        return testMainFilesMap;
    }

    //Generar el documento csv
    private File generateCsvInput(Map<String,String> testMainFilesMap) throws MojoExecutionException{
        File csvFile = new File(project.getBuild().getDirectory(), "testsmells_input.csv");
        String projectName = project.getArtifactId();

        try(PrintWriter writer = new PrintWriter(new FileWriter(csvFile))){
            for(Map.Entry<String, String> entry : testMainFilesMap.entrySet()){
                String testPath = entry.getKey();
                String mainPath = entry.getValue() != null ? entry.getValue() : "";

                writer.println(projectName + "," + testPath + "," + mainPath);
            }
        }catch (Exception e){
            throw new MojoExecutionException("Error generating CSV input file: ",e);
        }

        return csvFile;
    }

    //Ejecutar tsDetect
    private File executeTestSmellDetector(File csvInputFile) throws MojoExecutionException{
        try{
            File jarFile = getTestSmellDetectorJar();

            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                jarFile.getAbsolutePath(),
                csvInputFile.getAbsolutePath()
            );

            pb.directory(new File(project.getBuild().getDirectory()));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while (reader.readLine() != null) {} 
            } finally {
                if(reader != null) {
                    reader.close();
                } 
            }

            process.waitFor();
            File outputFile = findTestSmellDetectorOutput();

            return outputFile;
        }catch (Exception e){
            throw new MojoExecutionException("Error executing TestSmellDetector: ",e);
        }
    }

    //Ejecutar tsDetect, o descargarla desde URL en caso necesario
    private File getTestSmellDetectorJar() throws MojoExecutionException{
        File cachedJar = new File(project.getBuild().getDirectory(),"TestSmellDetector.jar");

        if(!cachedJar.exists()){
            getLog().info("Downloading TestSmellDetector jar...");
            try {
                cachedJar.getParentFile().mkdir();
                try (InputStream in = (new URL(TSDETECT_JAR_URL)).openStream()){
                    Files.copy(in, cachedJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to download TestSmellDetector: ",e);
            }
        }

        return cachedJar;
    }

    //Encontrar el output generado por tsDetect
    private File findTestSmellDetectorOutput() throws MojoExecutionException{
        File baseDir = new File(project.getBuild().getDirectory());

        File[] outputFiles = baseDir.listFiles((dir, name) ->
            name.startsWith("Output_TestSmellDetection_") && name.endsWith(".csv"));

        if(outputFiles == null || outputFiles.length == 0){
            throw new MojoExecutionException("TestSmellDetector did not produce output file");
        }

        File newestOutputFile = outputFiles[0];
        for(int i = 1; i < outputFiles.length; i++){
            if(outputFiles[i].lastModified() > newestOutputFile.lastModified()){
                newestOutputFile = outputFiles[i];
            }
        }

        getLog().info("Found TestSmellDetecto output: " + newestOutputFile.getName());

        return newestOutputFile;
    }

    //Mostrar el resultado de tsDetect en la pantalla
    private void displayResults(File resultFile) throws MojoExecutionException{
        try (BufferedReader reader = new BufferedReader(new FileReader(resultFile))){
            String [] headers = reader.readLine().split(",");
            String line;
            int total = 0;

            getLog().info("------------------------------------------------------------------------");
            getLog().info("Test Smells Result");
            getLog().info("------------------------------------------------------------------------");

            while((line = reader.readLine()) != null){
                String [] values = line.split(",");

                //La información de test smells empieza apartir de index 7 - Columna 8
                for(int i = 7; i < values.length; i++){
                    int count = Integer.parseInt(values[i].trim());
                    if(count > 0){
                        getLog().info(headers[i].trim()+": "+count);
                        total += count;
                    }
                }
            }
            getLog().info("Total: "+total);
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading results", e);
        }
    }
}
