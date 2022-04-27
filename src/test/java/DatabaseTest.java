import static org.junit.jupiter.api.Assertions.assertFalse;

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
                waitForChanges.countDown();
                System.out.println("Hey look at my super cool changes");
                // Don't commit until the other thread has a chance to look at the uncommitted data
                waitForLooking.await();
                tx.commit();
            }

            return null;
        });

        // Wait until the other thread has made some changes
        waitForChanges.await();

        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
            assertFalse(nodes.hasNext());
            System.out.println("What changes?, I can't see anything");

            waitForLooking.countDown();
            tx.commit();
        }

        // Wait for the task to finish
        makingChanges.get();
    }

    @Test
    void myPinkProgrammingTestMultiThreadedThreads() throws InterruptedException {
        GraphDatabaseService database = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        CountDownLatch waitForChanges = new CountDownLatch(1);
        CountDownLatch waitForLooking = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            try (Transaction tx = database.beginTx()) {
                tx.createNode(awesomeLabel).setProperty(coolPropName, "propValue");
                waitForChanges.countDown();
                System.out.println("Hey look at my super cool changes");
                // Don't commit until the other thread has a chance to look at the uncommitted data
                waitForLooking.await();
                tx.commit();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

        // Wait until the other thread has made some changes
        waitForChanges.await();

        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(awesomeLabel);
            assertFalse(nodes.hasNext());
            System.out.println("What changes?, I can't see anything");

            waitForLooking.countDown();
            tx.commit();
        }

        // Wait for the thread to finish
        thread.join();
    }

    @Test
    void myPinkProgrammingTestSingleThreaded() {
        GraphDatabaseService database = dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        try (var tx1 = database.beginTx(); var tx2 = database.beginTx()) {
            tx1.createNode(awesomeLabel).setProperty(coolPropName, "propValue");
            ResourceIterator<Node> nodes = tx2.findNodes(awesomeLabel);
            assertFalse(nodes.hasNext());
        }
    }
}
