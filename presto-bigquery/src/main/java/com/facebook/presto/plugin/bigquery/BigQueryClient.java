/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.plugin.bigquery;

import com.facebook.presto.plugin.jdbc.BaseJdbcClient;
import com.facebook.presto.plugin.jdbc.BaseJdbcConfig;
import com.facebook.presto.plugin.jdbc.DriverConnectionFactory;
import com.facebook.presto.plugin.jdbc.JdbcColumnHandle;
import com.facebook.presto.plugin.jdbc.JdbcConnectorId;
import com.facebook.presto.plugin.jdbc.JdbcTableHandle;
import com.facebook.presto.plugin.jdbc.JdbcTypeHandle;
import com.facebook.presto.plugin.jdbc.ReadMapping;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.TableNotFoundException;
import com.facebook.presto.spi.type.Type;
import com.google.common.collect.ImmutableList;
import com.simba.googlebigquery.jdbc42.Driver;

import javax.inject.Inject;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.facebook.presto.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static com.facebook.presto.spi.type.RealType.REAL;

public class BigQueryClient
        extends BaseJdbcClient
{


    @Inject
    public BigQueryClient(JdbcConnectorId connectorId, BigQueryConfig config)
    {
        super(connectorId, jdbcConfig(config), "`", new DriverConnectionFactory(new Driver(), jdbcConfig(config)));
    }

    private static BaseJdbcConfig jdbcConfig(BigQueryConfig bqConfig) {
        return new BaseJdbcConfig()
                .setConnectionUrl("jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;Timeout=3000;OAuthType=3;AllowLargeResults=0;LogLevel=0;Timeout=3000;ProjectId=glispa-business-analytics;");
    }

    @Override
    protected String toSqlType(Type type) {
        if (REAL.equals(type)) {
            return "float";
        }
        return super.toSqlType(type);
    }

    @Override
    public List<JdbcColumnHandle> getColumns(ConnectorSession session, JdbcTableHandle tableHandle)
    {
        try (Connection connection = connectionFactory.openConnection()) {
            try (ResultSet resultSet = getColumns(tableHandle, connection.getMetaData())) {
                List<JdbcColumnHandle> columns = new ArrayList<>();
                while (resultSet.next()) {
                    Object value = resultSet.getObject("DECIMAL_DIGITS");
                    JdbcTypeHandle typeHandle = new JdbcTypeHandle(
                            resultSet.getInt("DATA_TYPE"),
                            resultSet.getInt("COLUMN_SIZE"),
                            (value!=null)?(int)value:0);
                    Optional<ReadMapping> columnMapping = toPrestoType(session, typeHandle);
                    // skip unsupported column types
                    if (columnMapping.isPresent()) {
                        String columnName = resultSet.getString("COLUMN_NAME");
                        columns.add(new JdbcColumnHandle(connectorId, columnName, typeHandle, columnMapping.get().getType()));
                    }
                }
                if (columns.isEmpty()) {
                    // In rare cases (e.g. PostgreSQL) a table might have no columns.
                    throw new TableNotFoundException(tableHandle.getSchemaTableName());
                }
                return ImmutableList.copyOf(columns);
            }
        }
        catch (SQLException e) {
            throw new PrestoException(JDBC_ERROR, e);
        }
    }

    private static ResultSet getColumns(JdbcTableHandle tableHandle, DatabaseMetaData metadata)
            throws SQLException
    {
        String escape = metadata.getSearchStringEscape();
        return metadata.getColumns(
                tableHandle.getCatalogName(),
                escapeNamePattern(tableHandle.getSchemaName(), escape),
                escapeNamePattern(tableHandle.getTableName(), escape),
                null);
    }
}
