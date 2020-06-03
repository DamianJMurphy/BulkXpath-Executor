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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Damian Murphy
 */
public class OutputManager {
    
    private static final int STDOUT = 0;
    private static final int STDERR = 1;
    private static final int SINGLEFILE = 2;
    private static final int MULTIFILE = 3;
    private static final int MEMORY = 4;
    
    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
    
    private int outputMethod = STDOUT;
    private int errorMethod = STDERR;
    
    private boolean prependFile = false;
    private boolean timestamp = false;
    
    private String outputExtension = ".out";
    private String outputFileName = null;    
    private ArrayList<String> outputList = null;

    private String errorExtension = ".err";
    private String errorFileName = null;    
    private ArrayList<String> errorList = null;
    
    private PrintStream currentOutputStream = System.out;
    private PrintStream currentErrorStream = System.err;
    
    private String currentFileName = null;
    
    public OutputManager() {}
    
    public void setInMemoryOutput() {
        outputList = new ArrayList<>();
        outputMethod = MEMORY;
    }
    public void setInMemoryError() {
        errorList = new ArrayList<>();
        errorMethod = MEMORY;
    }
    
    public void setOutputExtension(String e) {
        outputExtension = e;
        outputMethod = MULTIFILE;
        currentOutputStream = null;
    }
    public void setErrorExtension(String e) {
        errorExtension = e;
        errorMethod = MULTIFILE;
        currentErrorStream = null;
    }
    
    public void setCurrentFile(String s) 
            throws Exception
    {
        currentFileName = s;
        if (outputMethod == MULTIFILE) {
            if (currentOutputStream != null) {
                currentOutputStream.flush();
                currentOutputStream.close();
            }
            String fname = s + outputExtension;
            currentOutputStream = new PrintStream(new FileOutputStream(fname));
        }
        if (errorMethod == MULTIFILE) {
            if (currentErrorStream != null) {
                currentErrorStream.flush();
                currentErrorStream.close();
            }
            String fname = s + errorExtension;
            currentErrorStream = new PrintStream(new FileOutputStream(fname));
        }
    }
    
    public void setPrependFilenameToError(boolean b) {
        prependFile = b; 
    }
    
    public void setTimestampError(boolean b) {
        timestamp = b;
    }
    
    public void setOutputFile(String s) 
            throws Exception
    {        
        if (s == null || s.trim().isEmpty()) {
            outputFileName = null;
            currentOutputStream = System.out;
            outputMethod = STDOUT;
        } else {
            outputFileName = s;
            currentOutputStream = new PrintStream(new FileOutputStream(outputFileName));
            outputMethod = SINGLEFILE;            
        }
    }

    public void setErrorFile(String s) 
            throws Exception
    {        
        if (s == null || s.trim().isEmpty()) {
            errorFileName = null;
            currentErrorStream = System.err;
            errorMethod = STDERR;
        } else {
            errorFileName = s;
            currentErrorStream = new PrintStream(new FileOutputStream(errorFileName));
            errorMethod = SINGLEFILE;            
        }
    }
    
    public void close() 
            throws Exception
    {
        if ((outputMethod == SINGLEFILE) || (outputMethod == MULTIFILE)) {
            if (currentOutputStream != null) {
                currentOutputStream.flush();
                currentOutputStream.close();
            }
        }
        if ((errorMethod == SINGLEFILE) || (errorMethod == MULTIFILE)) {
            if (currentErrorStream != null) {
                currentErrorStream.flush();
                currentErrorStream.close();
            }
        }
    }
    
    public ArrayList<String> getOutputs() { return outputList; }
    public ArrayList<String> getErrors() { return errorList; }
    
    public void error(String s)
            throws Exception
    {
        StringBuilder sb = new StringBuilder();
        if (prependFile) {
            sb.append(currentFileName);
            sb.append("\n");
        }
        if (timestamp) {
            sb.append(DATEFORMAT.format(new Date()));
            sb.append("\n");
        }
        sb.append(s);
        if (errorMethod == MEMORY) {
            if (errorList == null)
                errorList = new ArrayList<>();
            errorList.add(sb.toString());
        } else {
            currentErrorStream.println(sb.toString());
        }        
    }
    
    public void output(String s)
            throws Exception
    {
        // debuggers show this as unreadable marked as UTF-16 in the text but it is actually utf-8
        s = s.replaceFirst("encoding=\"UTF-16\"","encoding=\"UTF-8\"");
        if (outputMethod == MEMORY) {
            if (outputList == null)
                outputList = new ArrayList<>();
            outputList.add(s);
        } else {
            currentOutputStream.println(s);
        }
    }
}
