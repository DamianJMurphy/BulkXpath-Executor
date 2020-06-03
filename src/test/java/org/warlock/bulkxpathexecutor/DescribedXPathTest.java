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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
public class DescribedXPathTest {

    private DescribedXPath instance = null;
    private XPathExpression xpe = null;

    public DescribedXPathTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() throws XPathExpressionException {
        XPath xp = XPathFactory.newInstance().newXPath();
        xpe = xp.compile("/");
        instance = new DescribedXPath("/", xpe);
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of getXpath method, of class DescribedXPath.
     */
    @Test
    public void testGetXpath() {
        System.out.println("getXpath");
        String expResult = "/";
        String result = instance.getXpath();
        assertEquals(expResult, result);
    }

    /**
     * Test of getExpression method, of class DescribedXPath.
     * @throws javax.xml.xpath.XPathExpressionException
     */
    @Test
    public void testGetExpression() throws XPathExpressionException {
        System.out.println("getExpression");
        XPathExpression expResult = xpe;
        XPathExpression result = instance.getExpression();
        assertEquals(expResult, result);
    }

}
