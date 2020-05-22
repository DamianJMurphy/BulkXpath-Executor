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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
public class BulkXpathExecutorTest {

    private File outputFile = null;

    public BulkXpathExecutorTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        outputFile = new File("src/test/resources/output.xml");
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    @AfterEach
    public void tearDown() {
        outputFile.delete();
    }

    /**
     * Test of main method, of class BulkXpathExecutor.
     */
    @Test
    public void testMain() throws FileNotFoundException, IOException {
        System.out.println("main");

        String[] args = new String[]{"-p", "src/test/resources/locations.txt", "-r", "src/test/resources/data.txt", "-o", outputFile.getPath(), "src/test/resources/problems_resp.xml"};
        BulkXpathExecutor.main(args);
        assertTrue(outputFile.exists());
        try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
            String line = br.readLine();
            assertTrue(line.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        }
    }

}
