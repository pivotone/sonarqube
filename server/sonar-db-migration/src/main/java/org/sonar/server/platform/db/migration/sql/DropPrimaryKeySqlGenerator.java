/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class DropPrimaryKeySqlGenerator {
  private static final String GENERIC_DROP_CONSTRAINT_STATEMENT = "ALTER TABLE %s DROP CONSTRAINT %s";

  private final Database db;
  private final DbPrimaryKeyConstraintFinder dbConstraintFinder;

  public DropPrimaryKeySqlGenerator(Database db, DbPrimaryKeyConstraintFinder dbConstraintFinder) {
    this.db = db;
    this.dbConstraintFinder = dbConstraintFinder;
  }

  public List<String> generate(String tableName, String columnName, boolean isAutoGenerated) throws SQLException {
    return generate(tableName, singleton(columnName), isAutoGenerated);
  }

  public List<String> generate(String tableName, Collection<String> columnNames, boolean isAutoGenerated) throws SQLException {
    Dialect dialect = db.getDialect();
    String constraintName = dbConstraintFinder.findConstraintName(tableName);
    switch (dialect.getId()) {
      case PostgreSql.ID:
        return generateForPostgresSql(tableName, columnNames, constraintName);
      case MsSql.ID:
        return generateForMsSql(tableName, constraintName);
      case Oracle.ID:
        return generateForOracle(tableName, constraintName, isAutoGenerated);
      case H2.ID:
        return generateForH2(tableName, constraintName);
      default:
        throw new IllegalStateException(format("Unsupported database '%s'", dialect.getId()));
    }
  }

  private List<String> generateForPostgresSql(String tableName, Collection<String> columns, String constraintName) throws SQLException {
    List<String> statements = new ArrayList<>();
    for (String column : columns) {
      statements.add(format("ALTER TABLE %s ALTER COLUMN %s DROP DEFAULT", tableName, column));
      String sequence = dbConstraintFinder.getPostgresSqlSequence(tableName, column);
      if (sequence != null) {
        statements.add(format("DROP SEQUENCE %s", sequence));
      }
    }

    statements.add(format(GENERIC_DROP_CONSTRAINT_STATEMENT, tableName, constraintName));
    return statements;
  }

  private static List<String> generateForOracle(String tableName, String constraintName, boolean isAutoGenerated) {
    List<String> statements = new ArrayList<>();
    if (isAutoGenerated) {
      statements.add(format("DROP TRIGGER %s_IDT", tableName));
      statements.add(format("DROP SEQUENCE %s_SEQ", tableName));
    }

    // 'drop index' at the end ensures that associated index with primary key will be deleted
    statements.add(format(GENERIC_DROP_CONSTRAINT_STATEMENT + " DROP INDEX", tableName, constraintName));
    return statements;
  }

  private static List<String> generateForMsSql(String tableName, String constraintName) {
    return singletonList(format(GENERIC_DROP_CONSTRAINT_STATEMENT, tableName, constraintName));
  }

  private static List<String> generateForH2(String tableName, String constraintName) {
    return singletonList(format(GENERIC_DROP_CONSTRAINT_STATEMENT, tableName, constraintName));
  }

}
