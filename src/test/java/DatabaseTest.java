import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class DatabaseTest
{
    private static final Label awesomeLabel = Label.label( "awesomeLabel" );
    private static final String coolPropName = "coolPropertyName";
    private DatabaseManagementService dbms;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() throws IOException {
        Path path = Files.createTempDirectory("myTempFolder");
        dbms = new DatabaseManagementServiceBuilder(path).build();
        executorService = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        if (dbms != null) {
            dbms.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Test
    void myPinkProgrammingTestMultiThreadedExecutorService() throws ExecutionException, InterruptedException {
        GraphDatabaseService database = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        CountDownLatch waitForChanges = new CountDownLatch(1);
        CountDownLatch waitForLooking = new CountDownLatch(1);

        Future<?> makingChanges = executorService.submit(() -> {
            try (Transaction tx = database.beginTx()) {
                tx.createNode(awesomeLabel).setProperty(coolPropName, "propValue");
                tx.commit();
            }
            System.out.println("Hey look at my super cool changes");
            waitForChanges.countDown();
            waitForLooking.await();

            try (Transaction tx = database.beginTx()) {
                ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
                assertTrue(nodes.hasNext());
                System.out.println("I can see my own changes");
                tx.commit();
            }
            return null;
        });

        waitForChanges.await();

        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
            assertTrue(nodes.hasNext());
            Node node = nodes.next();
            System.out.println("Ooh cool, I really like your property " + node.getProperty(coolPropName));
            tx.commit();
        }
        waitForLooking.countDown();

        // Wait for the task to finish
        makingChanges.get();
    }

    @Test
    void myPinkProgrammingTestMultiThreadedThread() throws InterruptedException {
        GraphDatabaseService database = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        CountDownLatch waitForChanges = new CountDownLatch(1);
        CountDownLatch waitForLooking = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            try (Transaction tx = database.beginTx()) {
                tx.createNode(awesomeLabel).setProperty(coolPropName, "propValue");
                tx.commit();
            }
            System.out.println("Hey look at my super cool changes");
            waitForChanges.countDown();
            try {
                waitForLooking.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try (Transaction tx = database.beginTx()) {
                ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
                assertTrue(nodes.hasNext());
                System.out.println("I can see my own changes");
                tx.commit();
            }
        });
        thread.start();

        waitForChanges.await();

        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
            assertTrue(nodes.hasNext());
            Node node = nodes.next();
            System.out.println("Ooh cool, I really like your property " + node.getProperty(coolPropName));
            tx.commit();
        }
        waitForLooking.countDown();

        // Wait for the task to finish
        thread.join();
    }

    @Test
    void myPinkProgrammingRollbackTest() throws ExecutionException, InterruptedException {
        GraphDatabaseService database = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        CountDownLatch waitForChanges = new CountDownLatch(1);
        CountDownLatch waitForLooking = new CountDownLatch(1);

        Future<?> makingChanges = executorService.submit(() -> {
            try (Transaction tx = database.beginTx()) {
                tx.createNode(awesomeLabel).setProperty(coolPropName, "propValue");
                tx.rollback();
            }
            waitForChanges.countDown();
            waitForLooking.await();

            try (Transaction tx = database.beginTx()) {
                ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
                assertFalse(nodes.hasNext());
                System.out.println("Nothing to see here");
                tx.commit();
            }
            return null;
        });
        waitForChanges.await();

        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
            assertFalse(nodes.hasNext());
            System.out.println("Can't see anything");
            tx.commit();
        }
        waitForLooking.countDown();

        // Wait for the task to finish
        makingChanges.get();
    }
}
