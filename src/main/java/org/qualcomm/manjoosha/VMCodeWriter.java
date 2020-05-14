package org.qualcomm.manjoosha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EnumMap;

public class VMCodeWriter {

    private static final EnumMap<SEGMENT, String> SEGMENT_TO_STRING = new EnumMap<>(SEGMENT.class);
    private static final EnumMap<COMMAND, String> COMMAND_TO_STRING = new EnumMap<>(COMMAND.class);

    static {
        SEGMENT_TO_STRING.put(SEGMENT.CONST, "constant");
        SEGMENT_TO_STRING.put(SEGMENT.ARG, "argument");
        SEGMENT_TO_STRING.put(SEGMENT.LOCAL, "local");
        SEGMENT_TO_STRING.put(SEGMENT.STATIC, "static");
        SEGMENT_TO_STRING.put(SEGMENT.THIS, "this");
        SEGMENT_TO_STRING.put(SEGMENT.THAT, "that");
        SEGMENT_TO_STRING.put(SEGMENT.POINTER, "pointer");
        SEGMENT_TO_STRING.put(SEGMENT.TEMP, "temp");

        COMMAND_TO_STRING.put(COMMAND.ADD, "add");
        COMMAND_TO_STRING.put(COMMAND.SUB, "sub");
        COMMAND_TO_STRING.put(COMMAND.NEG, "neg");
        COMMAND_TO_STRING.put(COMMAND.EQ, "eq");
        COMMAND_TO_STRING.put(COMMAND.GT, "gt");
        COMMAND_TO_STRING.put(COMMAND.LT, "lt");
        COMMAND_TO_STRING.put(COMMAND.AND, "and");
        COMMAND_TO_STRING.put(COMMAND.OR, "or");
        COMMAND_TO_STRING.put(COMMAND.NOT, "not");
    }

    private PrintWriter printWriter;
    
    public VMCodeWriter(File fOut) {
        try {
            printWriter = new PrintWriter(fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * writes a VM push command
     *
     * @param segment
     * @param index
     */
    public void writePush(SEGMENT segment, int index) {
        writeCommand("push", SEGMENT_TO_STRING.get(segment), String.valueOf(index));
    }

    /**
     * writes a VM pop command
     *
     * @param segment
     * @param index
     */
    public void writePop(SEGMENT segment, int index) {
        writeCommand("pop", SEGMENT_TO_STRING.get(segment), String.valueOf(index));
    }

    /**
     * writes a VM arithmetic command
     *
     * @param command
     */
    public void writeArithmetic(COMMAND command) {
        writeCommand(COMMAND_TO_STRING.get(command), "", "");
    }

    /**
     * writes a VM label command
     *
     * @param label
     */
    public void writeLabel(String label) {
        writeCommand("label", label, "");
    }

    /**
     * writes a VM goto command
     *
     * @param label
     */
    public void writeGoto(String label) {
        writeCommand("goto", label, "");
    }

    /**
     * writes a VM if-goto command
     *
     * @param label
     */
    public void writeIf(String label) {
        writeCommand("if-goto", label, "");
    }

    /**
     * writes a VM call command
     *
     * @param name
     * @param nArgs
     */
    public void writeCall(String name, int nArgs) {
        writeCommand("call", name, String.valueOf(nArgs));
    }

    /**
     * writes a VM function command
     *
     * @param name
     * @param nLocals
     */
    public void writeFunction(String name, int nLocals) {
        writeCommand("function", name, String.valueOf(nLocals));
    }

    /**
     * writes a VM return command
     */
    public void writeReturn() {
        writeCommand("return", "", "");
    }

    public void writeCommand(String cmd, String arg1, String arg2) {

        printWriter.print(cmd + " " + arg1 + " " + arg2 + "\n");

    }

    /**
     * close the output file
     */
    public void close() {
        printWriter.close();
    }

    public enum SEGMENT {CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP, NONE}

    public enum COMMAND {ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT}
}

