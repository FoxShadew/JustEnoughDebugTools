package dev.runefox.jedt.test;

import com.google.common.base.Stopwatch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestReporter;

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

// Works with GitHub Actions JUnit reporter action
@Deprecated
public class ProperJUnitLikeTestReporter implements TestReporter {
    private final Document document;
    private final Element testSuite;
    private final Stopwatch stopwatch;
    private final File destination;
    private int failures;
    private int skips;
    private int successes;

    public ProperJUnitLikeTestReporter(File dest) throws ParserConfigurationException {
        this.destination = dest;
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        this.testSuite = document.createElement("testsuite");

        Element testSuites = document.createElement("testsuites");
        testSuites.appendChild(testSuite);
        document.appendChild(testSuites);

        testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        stopwatch = Stopwatch.createStarted();
    }

    private Element createTestCase(GameTestInfo testInfo, String string) {
        Element testCase = document.createElement("testcase");
        testCase.setAttribute("name", string);
        testCase.setAttribute("classname", testInfo.getStructureName());
        testCase.setAttribute("time", String.valueOf(testInfo.getRunTime() / 1000d));
        testSuite.appendChild(testCase);
        return testCase;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onTestFailed(GameTestInfo testInfo) {
        String name = testInfo.getTestName();
        String errorMsg = testInfo.getError().getMessage();

        Element failure; // "I'm a failure :("
        if (testInfo.isRequired()) {
            failures++;
            failure = document.createElement("failure");
        } else {
            skips++;
            failure = document.createElement("skipped");
        }
        failure.setAttribute("message", errorMsg);

        Element testCase = createTestCase(testInfo, name);
        testCase.appendChild(failure);
    }

    @Override
    public void onTestSuccess(GameTestInfo testInfo) {
        successes++;
        String testName = testInfo.getTestName();
        createTestCase(testInfo, testName);
    }

    @Override
    public void finish() {
        stopwatch.stop();

        testSuite.setAttribute("tests", "" + (failures + skips + successes));
        testSuite.setAttribute("name", "root");
        testSuite.setAttribute("failures", "" + failures);
        testSuite.setAttribute("skipped", "" + skips);
        testSuite.setAttribute("time", String.valueOf(stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000d));

        try {
            save(destination);
        } catch (TransformerException exc) {
            throw new Error("Couldn't save test report", exc);
        }
    }

    public void save(File file) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
}
