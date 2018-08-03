package com.gtc.opportunity.trader.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.tool.schema.extract.internal.SequenceInformationImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialect to handle incompatibilities with H2 and MySql. Also, this dialect handles special cases of compatibility
 * of tools like Flyway, Hibernate and H2 when H2 is running in MySQL compatibility mode.
 *
 * <p>The inner class SequenceInformationExtractorH2DatabaseImpl is a straight copy of the same class found in
 * Hibernate source code, that has been modified to work correctly with H2 running in MySQL compatibility mode and
 * DATABASE_TO_UPPER option set to false (this option must be false, for Flyway to be able to perform the clean
 * operation on the database - this is needed in our integration tests). The only modification is that in the code
 * this class uses "INFORMATION_SCHEMA" instead of "information_schema". Maybe, one day, Flyway and Hibernate will
 * fully support H2 in MySQL compatibility mode. Right now, Flyway needs DATABASE_TO_UPPER=false, and Hibernate
 * needs DATABASE_TO_UPPER=true. This little fix makes everybody work together nicely.
 */
@Slf4j
public class CustomH2Dialect extends org.hibernate.dialect.H2Dialect {

    public CustomH2Dialect() {
        registerColumnType(java.sql.Types.BOOLEAN, "TINYINT");
        // mysql compat.
        registerFunction("DATEDIFF", new SQLFunctionTemplate(StandardBasicTypes.STRING, "DATEDIFF('DAY',?2, ?1)"));
    }

    @Override
    public String getQuerySequencesString() {
        return "select sequence_name from INFORMATION_SCHEMA.sequences";
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
    }

    public static class SequenceInformationExtractorH2DatabaseImpl implements SequenceInformationExtractor {

        static final SequenceInformationExtractorH2DatabaseImpl INSTANCE =
                new SequenceInformationExtractorH2DatabaseImpl();

        @Override
        public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {

            final Statement statement = extractionContext.getJdbcConnection().createStatement();
            try {
                ResultSet resultSet =
                        statement.executeQuery("select SEQUENCE_CATALOG, SEQUENCE_SCHEMA, SEQUENCE_NAME, INCREMENT "
                                + "from INFORMATION_SCHEMA.sequences");
                try {
                    return extractResultSet(resultSet, extractionContext);
                } finally {
                    try {
                        resultSet.close();
                    } catch (SQLException ignore) {
                        log.error("SQL exception.", ignore);
                    }
                }
            } finally {
                try {
                    statement.close();
                } catch (SQLException ignore) {
                    log.error("SQL exception.", ignore);
                }
            }
        }

        private List<SequenceInformation> extractResultSet(ResultSet resultSet, ExtractionContext extractionContext)
                throws SQLException {
            final List<SequenceInformation> sequenceInformationList = new ArrayList<>();
            final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
            while (resultSet.next()) {
                sequenceInformationList.add(
                        new SequenceInformationImpl(
                                new QualifiedSequenceName(
                                        identifierHelper.toIdentifier(resultSet.getString("SEQUENCE_CATALOG")),
                                        identifierHelper.toIdentifier(resultSet.getString("SEQUENCE_SCHEMA")),
                                        identifierHelper.toIdentifier(resultSet.getString("SEQUENCE_NAME"))
                                ),
                                resultSet.getInt("INCREMENT")
                        )
                );
            }
            return sequenceInformationList;
        }
    }
}
