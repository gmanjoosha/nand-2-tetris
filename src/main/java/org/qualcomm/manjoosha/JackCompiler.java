package org.qualcomm.manjoosha;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JackCompiler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage:java JackCompiler [filename|directory]");
        } else {
            var inputFile = new File(args[0]);
            List<File> jackFiles = new ArrayList<>();
            if (inputFile.isFile()) {
                var path = inputFile.getAbsolutePath();
                if (isNotAJackFile(path)) {
                    throw new IllegalArgumentException(".jack file is required!");
                }
                jackFiles.add(inputFile);
            } else if (inputFile.isDirectory()) {
                jackFiles = readJackFilesInDir(inputFile);
                if (jackFiles.isEmpty()) {
                    throw new IllegalArgumentException("Jack file expected");
                }
            }
            jackFiles.stream().flatMap(jackFile -> Stream.of(jackFile)
                    .map(File::getAbsolutePath)
                    .map(path -> path.substring(0, path.lastIndexOf('.')) + ".vm")
                    .peek(fileOutPath -> System.out.println("File created : " + fileOutPath))
                    .map(File::new)
                    .map(outputFile -> new CompilationEngine(jackFile, outputFile))
            ).forEach(CompilationEngine::compileClass);
        }
    }

    private static boolean isNotAJackFile(String path) {
        return !path.endsWith(".jack");
    }

    public static List<File> readJackFilesInDir(File dir) {
        var files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".jack"))
                .collect(Collectors.toList());
    }

}
