/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.modules.screens;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.account.AccountPositionChangeTurnFormData;
import com.aimluck.eip.account.util.AccountUtils;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.util.ALEipUtils;

/**
 * ユーザーアカウントの順番情報を処理するクラスです。 <br />
 *
 */
public class AccountPositionChangeTurnFormScreen extends ALVelocityScreen {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(AccountPositionChangeTurnFormScreen.class.getName());

  /**
   *
   * @param rundata
   * @param context
   * @throws Exception
   */
  @Override
  protected void doOutput(RunData rundata, Context context) throws Exception {

    try {
      doAccountPosition_form(rundata, context);
    } catch (Exception ex) {
      logger.error("[AccountPositionChangeTurnFormScreen] Exception.", ex);
      ALEipUtils.redirectDBError(rundata);
    }
  }

  protected void doAccountPosition_form(RunData rundata, Context context) {
    // ユーザ情報の詳細画面や編集画面からの遷移時に，
    // セッションに残る ENTITY_ID を削除する．
    ALEipUtils.removeTemp(rundata, context, ALEipConstants.ENTITY_ID);

    AccountPositionChangeTurnFormData formData =
      new AccountPositionChangeTurnFormData();
    formData.initField();
    formData.doViewForm(this, rundata, context);
    String layout_template =
      "portlets/html/ajax-account-position-change-turn-form.vm";
    setTemplate(rundata, context, layout_template);
  }

  /**
   * @return
   */
  @Override
  protected String getPortletName() {
    return AccountUtils.ACCOUNT_POSITION_PORTLET_NAME;
  }
}