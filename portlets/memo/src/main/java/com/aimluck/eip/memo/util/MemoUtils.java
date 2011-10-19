/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
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

package com.aimluck.eip.memo.util;

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Parameter;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.om.profile.ProfileException;
import org.apache.jetspeed.om.profile.psml.PsmlParameter;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTMemo;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.util.ALEipUtils;

/**
 * Memoのユーティリティクラスです。 <BR>
 * 
 */
public class MemoUtils {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MemoUtils.class.getName());

  public static final String MEMO_PORTLET_NAME = "Memo";

  /**
   * Memo オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTMemo getEipTMemo(RunData rundata, Context context)
      throws ALPageNotFoundException {
    String memoid =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      if (memoid == null
        || "".equals(memoid)
        || Integer.valueOf(memoid) == null) {
        memoid = rundata.getParameters().getString(ALEipConstants.ENTITY_ID);
        if (memoid == null
          || "".equals(memoid)
          || Integer.valueOf(memoid) == null) {
          // Memo IDが空の場合
          logger.debug("[MemoUtils] Empty ID...");
          return null;
        }
      }
      EipTMemo memo = getEipTMemo(rundata, context, Integer.valueOf(memoid));
      return memo;
    } catch (ALPageNotFoundException pageNotFound) {
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 初期セレクトメモを設定します
   * 
   * @param rundata
   * @param index
   * @return
   */
  public static boolean saveMemoSelection(RunData rundata, String entityid) {
    String portletEntryId = rundata.getParameters().getString("js_peid", null);
    if (portletEntryId == null || "".equals(portletEntryId)) {
      return false;
    }

    String MEMO_IDX = "p1a-memos";

    try {
      Profile profile = ((JetspeedRunData) rundata).getProfile();
      Portlets portlets = profile.getDocument().getPortlets();
      if (portlets == null) {
        return false;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return false;
      }

      PsmlParameter param = null;
      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getId().equals(portletEntryId)) {
            boolean hasParam = false;
            Parameter params[] = entries[j].getParameter();
            int param_len = params.length;
            for (int k = 0; k < param_len; k++) {
              if (params[k].getName().equals(MEMO_IDX)) {
                params[k].setValue(entityid);
                entries[j].setParameter(k, params[k]);
                hasParam = true;
              }
            }

            if (!hasParam) {
              param = new PsmlParameter();
              param.setName(MEMO_IDX);
              param.setValue(entityid);
              entries[j].addParameter(param);
            }

            break;
          }
        }

      }

      profile.store();
      return true;
    } catch (IndexOutOfBoundsException e) {
      logger.error(e);
    } catch (ProfileException e) {
      logger.error(e);
    }
    return false;
  }

  /**
   * Memo オブジェクトモデルを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  public static EipTMemo getEipTMemo(RunData rundata, Context context,
      Integer memoid) throws ALPageNotFoundException {
    int uid = ALEipUtils.getUserId(rundata);
    try {
      if (memoid == null) {
        // Memo IDが空の場合
        logger.debug("[MemoUtils] Empty ID...");
        return null;
      }

      SelectQuery<EipTMemo> query = Database.query(EipTMemo.class);
      Expression exp =
        ExpressionFactory.matchDbExp(EipTMemo.MEMO_ID_PK_COLUMN, memoid);
      exp.andExp(ExpressionFactory.matchExp(EipTMemo.OWNER_ID_PROPERTY, Integer
        .valueOf(ALEipUtils.getUserId(rundata))));
      query.setQualifier(exp);
      List<EipTMemo> memos = query.fetchList();
      if (memos == null || memos.size() == 0) {
        // 指定したMemo IDのレコードが見つからない場合
        logger.debug("[MemoUtils] Not found ID...");
        return null;
      }

      // アクセス権の判定
      EipTMemo memo = memos.get(0);
      if (uid != memo.getOwnerId().intValue()) {
        logger.debug("[MemoUtils] Invalid user access...");
        throw new ALPageNotFoundException();
      }

      return memos.get(0);
    } catch (ALPageNotFoundException pageNotFound) {
      logger.error(pageNotFound);
      throw pageNotFound;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }
}
