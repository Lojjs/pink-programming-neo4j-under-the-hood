import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;

public class DatabaseTest
{
    private static final Label awesomeLabel = Label.label( "awesomeLabel" );
    private static final String coolPropName = "coolPropertyName";
    private DatabaseManagementService dbms;

    @BeforeEach
    void setUp() throws IOException {
        Path path = Files.createTempDirectory("myTempFolder");
        dbms = new DatabaseManagementServiceBuilder(path).build();
    }

    @AfterEach
    void tearDown() {
        if (dbms != null) {
            dbms.shutdown();
        }
    }

    @Test
    void myPinkProgrammingTest() {
        GraphDatabaseService database = dbms.database( GraphDatabaseSettings.DEFAULT_DATABASE_NAME );

        assertEquals(true,true);
    }
}
