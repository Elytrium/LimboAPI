/*
 * Copyright (C) 2021 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings("unused")
@DatabaseTable(tableName = "AUTH")
public class RegisteredPlayer {

  @DatabaseField(canBeNull = false, columnName = "NICKNAME")
  public String nickname;

  @DatabaseField(id = true, columnName = "LOWERCASENICKNAME")
  public String lowercaseNickname;

  @DatabaseField(canBeNull = false, columnName = "HASH")
  public String hash;

  @DatabaseField(columnName = "IP")
  public String ip;

  @DatabaseField(columnName = "TOTPTOKEN")
  public String totpToken;

  @DatabaseField(columnName = "REGDATE")
  public Long regDate;

  @DatabaseField(columnName = "UUID")
  public String uuid;

  @DatabaseField(columnName = "PREMIUMUUID")
  public String premiumUuid;

  public RegisteredPlayer(String nickname, String lowercaseNickname,
      String hash, String ip, String totpToken, Long regDate, String uuid, String premiumUuid) {
    this.nickname = nickname;
    this.lowercaseNickname = lowercaseNickname;
    this.hash = hash;
    this.ip = ip;
    this.totpToken = totpToken;
    this.regDate = regDate;
    this.uuid = uuid;
    this.premiumUuid = premiumUuid;
  }

  public RegisteredPlayer() {

  }
}
