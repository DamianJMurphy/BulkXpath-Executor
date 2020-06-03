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

import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author simonfarrow
 */
public class OutputManagerTest {

    private OutputManager instance = null;

    public OutputManagerTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        instance = new OutputManager();
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of setInMemoryOutput method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetInMemoryOutput() throws Exception {
        System.out.println("setInMemoryOutput");
        instance.setInMemoryOutput();
        String expResult = "test";
        instance.output(expResult);
        assertEquals(expResult, instance.getOutputs().get(0));
    }

    /**
     * Test of setInMemoryError method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetInMemoryError() throws Exception {
        System.out.println("setInMemoryError");
        instance.setInMemoryError();
        String expResult = "test";
        instance.error(expResult);
        assertEquals(expResult, instance.getErrors().get(0));
    }

    /**
     * Test of setOutputExtension method, of class OutputManager.
     */
    @Test
    public void testSetOutputExtension() {
        System.out.println("setOutputExtension");
        String e = "";
        instance.setOutputExtension(e);
    }

    /**
     * Test of setErrorExtension method, of class OutputManager.
     */
    @Test
    public void testSetErrorExtension() {
        System.out.println("setErrorExtension");
        String e = "";
        instance.setErrorExtension(e);
    }

    /**
     * Test of setCurrentFile method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetCurrentFile() throws Exception {
        System.out.println("setCurrentFile");
        String s = "";
        instance.setCurrentFile(s);
    }

    /**
     * Test of setPrependFilenameToError method, of class OutputManager.
     */
    @Test
    public void testSetPrependFilenameToError() {
        System.out.println("setPrependFilenameToError");
        boolean b = false;
        instance.setPrependFilenameToError(b);
    }

    /**
     * Test of setTimestampError method, of class OutputManager.
     */
    @Test
    public void testSetTimestampError() {
        System.out.println("setTimestampError");
        boolean b = false;
        instance.setTimestampError(b);
    }

    /**
     * Test of setOutputFile method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetOutputFile() throws Exception {
        System.out.println("setOutputFile");
        String s = "";
        instance.setOutputFile(s);
    }

    /**
     * Test of setErrorFile method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testSetErrorFile() throws Exception {
        System.out.println("setErrorFile");
        String s = "";
        instance.setErrorFile(s);
    }

    /**
     * Test of getErrors method, of class OutputManager.
     */
    @Test
    public void testGetErrors() {
        System.out.println("getErrors");
        ArrayList<String> expResult = null;
        ArrayList<String> result = instance.getErrors();
        assertEquals(expResult, result);
    }

    /**
     * Test of error method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testError() throws Exception {
        System.out.println("error");
        String s = "";
        instance.error(s);
    }

    /**
     * Test of output method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testOutput() throws Exception {
        System.out.println("output");
        String s = "";
        instance.output(s);
    }

    /**
     * Test of close method, of class OutputManager.
     * @throws java.lang.Exception
     */
    @Test
    public void testClose() throws Exception {
        System.out.println("close");
        instance.close();
    }

    /**
     * Test of getOutputs method, of class OutputManager.
     */
    @Test
    public void testGetOutputs() {
        System.out.println("getOutputs");
        ArrayList<String> expResult = null;
        ArrayList<String> result = instance.getOutputs();
        assertEquals(expResult, result);
    }

}
