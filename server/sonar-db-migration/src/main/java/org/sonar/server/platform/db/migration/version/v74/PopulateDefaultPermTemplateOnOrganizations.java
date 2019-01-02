/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class PopulateDefaultPermTemplateOnOrganizations extends DataChange {

  private final System2 system2;

  public PopulateDefaultPermTemplateOnOrganizations(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("organizations");
    massUpdate.select("SELECT uuid, default_perm_template_view FROM organizations WHERE default_perm_template_view IS NOT NULL");
    massUpdate.update("UPDATE organizations " +
      "SET default_perm_template_app=?, default_perm_template_port=?, updated_at=? " +
      "WHERE uuid=?");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      String defaultPermTemplateView = row.getString(2);
      update.setString(1, defaultPermTemplateView);
      update.setString(2, defaultPermTemplateView);
      update.setLong(3, now);
      update.setString(4, uuid);
      return true;
    });
  }
}
