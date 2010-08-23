/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.schedule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALCellDateField;
import com.aimluck.commons.field.ALCellDateTimeField;
import com.aimluck.commons.field.ALCellNumberField;
import com.aimluck.commons.field.ALCellStringField;
import com.aimluck.eip.cayenne.om.portlet.EipMFacility;
import com.aimluck.eip.cayenne.om.portlet.EipTSchedule;
import com.aimluck.eip.cayenne.om.portlet.EipTScheduleMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.FacilityResultData;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.DatabaseOrmService;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.CellScheduleUtils;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.util.ALEipUtils;

/**
 * スケジュールのフォームデータを管理するクラスです。
 * 
 */
public class CellScheduleFormDateData extends ALAbstractFormData {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(CellScheduleFormDateData.class.getName());

  /** <code>FLAG_EDIT_REPEAT_DEF</code> デフォルト値（繰り返し編集範囲） */
  private static final int FLAG_EDIT_REPEAT_DEF = -1;

  /** <code>FLAG_EDIT_REPEAT_ALL</code> 個別日程を編集（繰り返し編集範囲） */
  private static final int FLAG_EDIT_REPEAT_ALL = 0;

  /** <code>FLAG_EDIT_SCHEDULE_ONE</code> 全日程を編集（繰り返し編集範囲） */
  // private static final int FLAG_EDIT_REPEAT_ONE = 1;
  /** <code>FLAG_DEL_MEMBER_ALL</code> [削除フラグ] すべての共有メンバーからこのスケジュールを削除する */
  public static final int FLAG_DEL_MEMBER_ALL = 0;

  /** <code>FLAG_DEL_MEMBER_ONE</code> [削除フラグ] このスケジュールからログインユーザーだけを削除する */
  public static final int FLAG_DEL_MEMBER_ONE = 1;

  /** <code>FLAG_DEL_RANGE_ALL</code> [削除フラグ] この繰り返しスケジュールを削除する */
  public static final int FLAG_DEL_RANGE_ALL = 0;

  /** <code>FLAG_DEL_RANGE_ONE</code> [削除フラグ] この繰り返しスケジュールの特定スケジュールのみを削除する */
  public static final int FLAG_DEL_RANGE_ONE = 1;

  /** <code>start_date</code> 開始日時 */
  private ALCellDateTimeField start_date;

  /** <code>end_date</code> 終了日時 */
  private ALCellDateTimeField end_date;

  /** <code>repeat_type</code> 繰り返し種別 */
  private ALCellStringField repeat_type;

  /** <code>week_0</code> 繰り返し曜日 */
  private ALCellStringField week_0;

  /** <code>week_1</code> 繰り返し曜日 */
  private ALCellStringField week_1;

  /** <code>week_2</code> 繰り返し曜日 */
  private ALCellStringField week_2;

  /** <code>week_3</code> 繰り返し曜日 */
  private ALCellStringField week_3;

  /** <code>week_4</code> 繰り返し曜日 */
  private ALCellStringField week_4;

  /** <code>week_5</code> 繰り返し曜日 */
  private ALCellStringField week_5;

  /** <code>week_6</code> 繰り返し曜日 */
  private ALCellStringField week_6;

  /** <code>limit_flag</code> 期限ありなし */
  private ALCellStringField limit_flag;

  /** <code>limit_date</code> 繰り返し期限（開始日） */
  private ALCellDateField limit_start_date;

  /** <code>limit_date</code> 繰り返し期限（終了日） */
  private ALCellDateField limit_end_date;

  /** <code>month_day</code> 繰り返す日 */
  private ALCellNumberField month_day;

  /** <code>memberList</code> メンバーリスト */
  private List<ALEipUser> memberList;

  /** <code>memberList</code> 施設メンバーリスト */
  private List<FacilityResultData> facilityMemberList;

  /** <code>currentYear</code> 現在の年 */
  private int currentYear;

  /** <code>is_member</code> スケジュールを共有するかどうか */
  private boolean is_member;

  /** <code>is_repeat</code> 繰り返すかどうか */
  private boolean is_repeat;

  /** <code>is_span</code> 期間指定かどうか */
  private boolean is_span;

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /** <code>tmpStart</code> 開始日時の初期値 */
  private String tmpStart;

  /** <code>tmpEnd</code> 終了日時の初期値 */
  private String tmpEnd;

  /** <code>selectData</code> 編集するスケジュールの1日の情報 */
  private ScheduleOnedayGroupSelectData selectData;

  /** <code>edit_schedule_flag</code> 繰り返しスケジュールの編集フラグ */
  private ALCellNumberField edit_repeat_flag;

  /** <code>tmpView</code> 表示する日 */
  private String tmpView;

  /** <code>end_date</code> 終了日時 */
  private ALCellDateTimeField view_date;

  /** <code>edit_control_flag</code> 共有メンバーによる編集／削除権限フラグ */
  private ALCellStringField edit_flag;

  /** <code>enable_change_tmpreserve</code> 仮スケジュールに戻すかどうか */
  private ALCellStringField change_tmpreserve_flag;

  /** <code>isOwner</code> 所有者かどうか */
  private boolean is_owner;

  protected String entityid;

  private String aclPortletFeature;

  /**
   * フォームを表示します。
   * 
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doViewForm(ALAction action, RunData rundata, Context context,
      boolean enable_entityid) {
    if (enable_entityid) {
      return super.doViewForm(action, rundata, context);
    } else {
      try {
        init(action, rundata, context);
        // action.setMode(ALEipConstants.MODE_NEW_FORM);
        // setMode(action.getMode());
        // this.setMode(action.getMode());
        // ArrayList msgList = new ArrayList();
        // boolean res = setFormData(rundata, context, msgList);
        boolean isedit =
          (ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID) != null);
        action.setMode(isedit
          ? ALEipConstants.MODE_EDIT_FORM
          : ALEipConstants.MODE_NEW_FORM);
        setMode(action.getMode());
        List<String> msgList = new ArrayList<String>();
        boolean res =
          (setFormData(rundata, context, msgList) && ScheduleUtils
            .validateDelegate(
              getStartDate(),
              getEndDate(),
              getRepeatType(),
              isRepeat(),
              isSpan(),
              getWeek0(),
              getWeek1(),
              getWeek2(),
              getWeek3(),
              getWeek4(),
              getWeek5(),
              getWeek6(),
              getLimitFlag(),
              getLimitStartDate(),
              getLimitEndDate(),
              getMonthDay(),
              getLoginUser(),
              entityid,
              msgList));
        action.setResultData(this);
        action.addErrorMessages(msgList);
        action.putData(rundata, context);
        return res;
      } catch (ALPageNotFoundException e) {
        ALEipUtils.redirectPageNotFound(rundata);
        return false;
      } catch (ALDBErrorException e) {
        ALEipUtils.redirectDBError(rundata);
        return false;
      }
    }

  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#init(com.aimluck.eip.modules.
   * actions.common.ALAction, org.apache.turbine.util.RunData,
   * org.apache.velocity.context.Context)
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    login_user = ALEipUtils.getALEipUser(rundata);
    is_owner = true;

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);

    entityid = ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    aclPortletFeature = ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  /**
   * パラメータを読み込みます。
   * 
   * @param rundata
   * @param context
   */
  public void loadParameters(RunData rundata, Context context) {
    ScheduleUtils.loadParametersDelegate(
      rundata,
      context,
      tmpStart,
      tmpEnd,
      tmpView);
    tmpView = ALEipUtils.getTemp(rundata, context, "tmpView");

    ALEipUtils.setTemp(rundata, context, "tmpStart", tmpView + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", tmpView + "-00-00");
    tmpStart = ALEipUtils.getTemp(rundata, context, "tmpStart");
    tmpEnd = ALEipUtils.getTemp(rundata, context, "tmpEnd");
  }

  /*
   * @see com.aimluck.eip.common.ALData#initField()
   */
  public void initField() {
    Calendar cal = Calendar.getInstance();
    int min = cal.get(Calendar.MINUTE);
    if (min <= 15) {
      cal.set(Calendar.MINUTE, 15);
    } else if (min <= 30) {
      cal.set(Calendar.MINUTE, 30);
    } else if (min <= 45) {
      cal.set(Calendar.MINUTE, 45);
    } else {
      cal.set(Calendar.MINUTE, 60);
    }
    Date now = cal.getTime();
    // 開始日時
    start_date = new ALCellDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpStart == null || tmpStart.equals("")) {
      start_date.setValue(now);
    } else {
      start_date.setValue(tmpStart);
    }
    start_date.setFieldName("開始日時");
    // 終了日時
    end_date = new ALCellDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpEnd == null || tmpEnd.equals("")) {
      if (tmpStart == null || tmpStart.equals("")) {
        end_date.setValue(now);
      } else {
        end_date.setValue(tmpStart);
      }
    } else {
      end_date.setValue(tmpEnd);
    }
    end_date.setFieldName("終了日時");
    // 繰り返しタイプ
    repeat_type = new ALCellStringField();
    repeat_type.setFieldName("繰り返しタイプ");
    repeat_type.setValue("D");
    repeat_type.setTrim(true);
    // 日
    week_0 = new ALCellStringField();
    week_0.setFieldName("日");
    week_0.setTrim(true);
    // 月
    week_1 = new ALCellStringField();
    week_1.setFieldName("月");
    week_1.setTrim(true);
    // 火
    week_2 = new ALCellStringField();
    week_2.setFieldName("火");
    week_2.setTrim(true);
    // 水
    week_3 = new ALCellStringField();
    week_3.setFieldName("水");
    week_3.setTrim(true);
    // 木
    week_4 = new ALCellStringField();
    week_4.setFieldName("木");
    week_4.setTrim(true);
    // 金
    week_5 = new ALCellStringField();
    week_5.setFieldName("金");
    week_5.setTrim(true);
    // 土
    week_6 = new ALCellStringField();
    week_6.setFieldName("土");
    week_6.setTrim(true);
    // 繰り返し日（選択されたときのみ Validate する）
    month_day = new ALCellNumberField();
    month_day.setFieldName("毎月の日");
    month_day.limitValue(1, 31);
    // 繰り返しフラグ
    limit_flag = new ALCellStringField();
    limit_flag.setFieldName("繰り返し");
    limit_flag.setValue("OFF");
    limit_flag.setTrim(true);
    // 繰り返し期限
    limit_start_date = new ALCellDateField();
    limit_start_date.setFieldName("繰り返し期限");
    limit_start_date.setValue(start_date.getValue());
    limit_end_date = new ALCellDateField();
    limit_end_date.setFieldName("繰り返し期限");
    ALCellDateTimeField tmp_date = new ALCellDateTimeField("yyyy-MM-dd-HH-mm");
    if (tmpEnd == null || tmpEnd.equals("")) {
      if (tmpStart == null || tmpStart.equals("")) {
        tmp_date.setValue(now);
        limit_end_date.setValue(tmp_date.getValue());
      } else {
        tmp_date.setValue(tmpStart);
        limit_end_date.setValue(tmp_date.getValue());
      }
    } else {
      tmp_date.setValue(tmpEnd);
      limit_end_date.setValue(tmp_date.getValue());
    }

    // 繰り返しスケジュールの編集フラグ
    edit_repeat_flag = new ALCellNumberField();
    edit_repeat_flag.setFieldName("繰り返し編集範囲");
    edit_repeat_flag.setValue(FLAG_EDIT_REPEAT_DEF);

    // メンバーリスト
    memberList = new ArrayList<ALEipUser>();

    // 指定日時
    view_date = new ALCellDateTimeField("yyyy-MM-dd");
    if (tmpView == null || tmpView.equals("")) {
      view_date.setValue(now);
    } else {
      view_date.setValue(tmpView);
    }
    view_date.setFieldName("指定日時");

    // 共有メンバーによる編集／削除権限フラグ
    edit_flag = new ALCellStringField();
    edit_flag.setFieldName("編集／削除権限");
    edit_flag.setTrim(true);
    edit_flag.setValue("T");

    // 仮スケジュールに戻すかどうか
    change_tmpreserve_flag = new ALCellStringField();
    change_tmpreserve_flag.setFieldName("仮スケジュール");
    change_tmpreserve_flag.setTrim(true);
    change_tmpreserve_flag.setValue("F");

    // 現在の年（年を選択するリストボックスに利用）
    currentYear = Calendar.getInstance().get(Calendar.YEAR);

    facilityMemberList = new ArrayList<FacilityResultData>();
  }

  /*
   * @see com.aimluck.eip.common.ALAbstractFormData#setValidator()
   */
  @Override
  protected void setValidator() {
    getMonthDay().setNotNull(true);
    getLimitStartDate().setNotNull(true);
    getLimitEndDate().setNotNull(true);
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#validate(java.util.ArrayList)
   */
  @Override
  protected boolean validate(List<String> msgList) throws ALDBErrorException,
      ALPageNotFoundException {
    return ScheduleUtils.validateDelegate(
      getStartDate(),
      getEndDate(),
      getRepeatType(),
      isRepeat(),
      isSpan(),
      getWeek0(),
      getWeek1(),
      getWeek2(),
      getWeek3(),
      getWeek4(),
      getWeek5(),
      getWeek6(),
      getLimitFlag(),
      getLimitStartDate(),
      getLimitEndDate(),
      getMonthDay(),
      getLoginUser(),
      entityid,
      msgList);
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#loadFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTSchedule record =
        ScheduleUtils.getEipTSchedule(rundata, context, false);
      if (record == null) {
        return false;
      }

      is_owner =
        (record.getOwnerId().longValue() == login_user.getUserId().getValue())
          ? true
          : false;

      // 共有メンバーによる編集／削除フラグ
      if ("T".equals(record.getEditFlag())) {
        if (is_owner) {
          edit_flag.setValue(record.getEditFlag());
        } else {
          // スケジュールの登録ユーザがすでにメンバーから抜けているかを検証する．
          int createUserId = record.getOwnerId().intValue();
          boolean inculudeCreateUser = false;
          List<?> scheduleMaps = record.getEipTScheduleMaps();
          for (int i = 0; i < scheduleMaps.size(); i++) {
            EipTScheduleMap map = (EipTScheduleMap) scheduleMaps.get(i);
            if (createUserId == map.getUserId().intValue()
              && !"R".equals(map.getStatus())) {
              inculudeCreateUser = true;
              break;
            }
          }
          if (inculudeCreateUser) {
            edit_flag.setValue("F");
          } else {
            edit_flag.setValue("T");
          }
        }
      } else {
        edit_flag.setValue("F");
      }
      // DN -> 毎日 (A = N -> 期限なし A = L -> 期限あり)
      // WnnnnnnnN W01111110 -> 毎週(月～金用)
      // MnnN M25 -> 毎月25日
      // S -> 期間での指定
      String ptn = record.getRepeatPattern();
      int count = 0;
      is_repeat = true;
      is_span = false;
      // 毎日
      if (ptn.charAt(0) == 'D') {
        repeat_type.setValue("D");
        count = 1;
        // 毎週
      } else if (ptn.charAt(0) == 'W') {
        repeat_type.setValue("W");
        week_0.setValue(ptn.charAt(1) != '0' ? "TRUE" : null);
        week_1.setValue(ptn.charAt(2) != '0' ? "TRUE" : null);
        week_2.setValue(ptn.charAt(3) != '0' ? "TRUE" : null);
        week_3.setValue(ptn.charAt(4) != '0' ? "TRUE" : null);
        week_4.setValue(ptn.charAt(5) != '0' ? "TRUE" : null);
        week_5.setValue(ptn.charAt(6) != '0' ? "TRUE" : null);
        week_6.setValue(ptn.charAt(7) != '0' ? "TRUE" : null);
        count = 8;
        // 毎月
      } else if (ptn.charAt(0) == 'M') {
        repeat_type.setValue("M");
        month_day.setValue(Integer.parseInt(ptn.substring(1, 3)));
        count = 3;
        // 期間
      } else if (ptn.charAt(0) == 'S') {
        is_span = true;
        is_repeat = false;
      } else {
        is_repeat = false;
      }
      if (is_repeat) {
        // 開始日時
        Calendar tmpViewCal = Calendar.getInstance();
        tmpViewCal.setTime(view_date.getValue());
        Calendar tmpStartCal = Calendar.getInstance();
        tmpStartCal.setTime(record.getStartDate());
        tmpViewCal.set(Calendar.HOUR_OF_DAY, tmpStartCal
          .get(Calendar.HOUR_OF_DAY));
        tmpViewCal.set(Calendar.MINUTE, tmpStartCal.get(Calendar.MINUTE));
        start_date.setValue(tmpViewCal.getTime());
        // 終了日時
        Calendar tmpStopCal = Calendar.getInstance();
        tmpStopCal.setTime(record.getEndDate());
        tmpViewCal.set(Calendar.HOUR_OF_DAY, tmpStopCal
          .get(Calendar.HOUR_OF_DAY));
        tmpViewCal.set(Calendar.MINUTE, tmpStopCal.get(Calendar.MINUTE));
        end_date.setValue(tmpViewCal.getTime());

        if (ptn.charAt(count) == 'N') {
          limit_start_date.setValue(view_date.getValue());
          limit_end_date.setValue(view_date.getValue());
          limit_flag.setValue("OFF");
        } else {
          limit_flag.setValue("ON");
          limit_start_date.setValue(record.getStartDate());
          limit_end_date.setValue(record.getEndDate());
        }

        // 繰り返しスケジュールの編集フラグ
        edit_repeat_flag.setValue(FLAG_EDIT_REPEAT_ALL);
      }

      if (is_span) {
        // 開始日時
        start_date.setValue(record.getStartDate());
        // 終了日時
        end_date.setValue(record.getEndDate());

        limit_start_date.setValue(record.getStartDate());
        limit_end_date.setValue(record.getEndDate());
      }

      if (!is_repeat && !is_span) {
        // 開始日時
        start_date.setValue(record.getStartDate());
        // 終了日時
        end_date.setValue(record.getEndDate());

        limit_start_date.setValue(record.getStartDate());
        limit_end_date.setValue(record.getEndDate());
      }

      // このスケジュールを共有しているメンバーを取得
      DataContext dataContext =
        DatabaseOrmService.getInstance().getDataContext();
      SelectQuery mapquery = new SelectQuery(EipTScheduleMap.class);
      Expression mapexp =
        ExpressionFactory.matchExp(EipTScheduleMap.SCHEDULE_ID_PROPERTY, record
          .getScheduleId());
      mapquery.setQualifier(mapexp);
      @SuppressWarnings("unchecked")
      List<EipTScheduleMap> list = mapquery.fetchList();
      List<Integer> users = new ArrayList<Integer>();
      List<Integer> facilityIds = new ArrayList<Integer>();
      int size = list.size();
      for (int i = 0; i < size; i++) {
        EipTScheduleMap map = list.get(i);
        if (ScheduleUtils.SCHEDULEMAP_TYPE_USER.equals(map.getType())) {
          users.add(map.getUserId());
        } else {
          facilityIds.add(map.getUserId());
        }
      }
      if (users.size() > 0) {
        SelectQuery query = new SelectQuery(TurbineUser.class);
        Expression exp =
          ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users);
        query.setQualifier(exp);
        memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
      }
      if (facilityIds.size() > 0) {
        SelectQuery fquery = new SelectQuery(EipMFacility.class);
        Expression fexp =
          ExpressionFactory.inDbExp(
            EipMFacility.FACILITY_ID_PK_COLUMN,
            facilityIds);
        fquery.setQualifier(fexp);
        facilityMemberList.addAll(FacilitiesUtils
          .getFacilitiesFromSelectQuery(fquery));
      }
    } catch (Exception e) {

      // TODO: エラー処理
      logger.error("[ScheduleFormData]", e);
      throw new ALDBErrorException();

    }
    return true;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#insertFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALDBErrorException {
    return true;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#updateFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return false;
  }

  /**
   * 入力データを検証する．
   * 
   * @param action
   * @param rundata
   * @param context
   * @return TRUE 成功 FALSE 失敗
   */
  public boolean doCheck(ALAction action, RunData rundata, Context context) {
    try {
      init(action, rundata, context);
      // action.setMode(ALEipConstants.MODE_INSERT);
      List<String> msgList = new ArrayList<String>();
      setValidator();
      boolean res =
        (setFormData(rundata, context, msgList) && validate(msgList));
      if (!res) {
        action.setMode(ALEipConstants.MODE_NEW_FORM);
      }
      action.setResultData(this);
      action.addErrorMessages(msgList);
      action.putData(rundata, context);
      return res;
    } catch (ALPageNotFoundException e) {
      ALEipUtils.redirectPageNotFound(rundata);
      return false;
    } catch (ALDBErrorException e) {
      ALEipUtils.redirectDBError(rundata);
      return false;
    }
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#deleteFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return false;
  }

  /*
   * @see
   * com.aimluck.eip.common.ALAbstractFormData#setFormData(org.apache.turbine
   * .util.RunData, org.apache.velocity.context.Context, java.util.ArrayList)
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    Field[] fields = this.getClass().getDeclaredFields();
    boolean res =
      ScheduleUtils
        .setFormDataDelegate(rundata, context, this, fields, msgList);

    if (!res) {
      return res;
    }
    memberList = CellScheduleUtils.getShareUserMemberList(rundata);
    if (!is_span) {
      Date date = start_date.getValue();
      int year = date.getYear();
      int month = date.getMonth();
      int day = date.getDate();
      date = end_date.getValue();
      date.setYear(year);
      date.setMonth(month);
      date.setDate(day);
      end_date.setValue(date);
      facilityMemberList =
        CellScheduleUtils.getShareFacilityMemberList(rundata);
    }

    return true;
  }

  public void setScheduleType(boolean is_repeat, boolean is_span) {
    this.is_repeat = is_repeat;
    this.is_span = is_span;
  }

  public void setScheduleType(RunData rundata) {
    setScheduleType(rundata.getParameters().getBoolean("is_repeat"), rundata
      .getParameters()
      .getBoolean("is_span"));
  }

  /**
   * 開始日時を取得します。
   * 
   * @return
   */
  public ALCellDateTimeField getStartDate() {
    return start_date;
  }

  public String getStartDateDate() {
    return ScheduleUtils.translateDate(start_date.getValue(), "yyyyMMdd");
  }

  public String getStartDateTime() {
    return ScheduleUtils.translateDate(start_date.getValue(), "HHmm");
  }

  /**
   * 終了日時を取得します。
   * 
   * @return
   */
  public ALCellDateTimeField getEndDate() {
    return end_date;
  }

  public String getEndDateDate() {
    return ScheduleUtils.translateDate(end_date.getValue(), "yyyyMMdd");
  }

  public String getEndDateTime() {
    return ScheduleUtils.translateDate(end_date.getValue(), "HHmm");
  }

  /**
   * 指定したグループ名のユーザーを取得します。
   * 
   * @param groupname
   * @return
   */
  public List getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
  }

  /**
   * 部署マップを取得します。
   * 
   * @return
   */
  public Map getPostMap() {
    return ALEipManager.getInstance().getPostMap();
  }

  /**
   * 終了日時を取得します。
   * 
   * @return
   */
  public int getCurrentYear() {
    return currentYear;
  }

  /**
   * 
   * @return
   */
  public boolean isOwner() {
    return is_owner;
  }

  /**
   * ログインユーザを取得します。
   * 
   * @return
   */
  public ALEipUser getLoginUser() {
    return login_user;
  }

  /**
   * 編集するスケジュールの1日の情報を取得します。
   * 
   * @return
   */
  public ScheduleOnedayGroupSelectData getSelectData() {
    return selectData;
  }

  /**
   * 繰り返すかどうか。
   * 
   * @return
   */
  public boolean isRepeat() {
    return is_repeat;
  }

  /**
   * 期間スケジュールかどうか。
   * 
   * @return
   */
  public boolean isSpan() {
    return is_span;
  }

  /**
   * 期限を取得します。
   * 
   * @return
   */
  public ALCellDateField getLimitStartDate() {
    return limit_start_date;
  }

  public String getLimitStartDateDate() {
    Date date = null;
    try {
      date = limit_start_date.getValue().getDate();
    } catch (Exception e) {
    }
    if (date == null) {
      return "";
    }
    return ScheduleUtils.translateDate(date, "yyyyMMdd");
    // return ""+(date.getYear()+1900)+date.getMonth()+date.getDate();
  }

  /**
   * 期限を取得します。
   * 
   * @return
   */
  public ALCellDateField getLimitEndDate() {
    return limit_end_date;
  }

  public String getLimitEndDateDate() {
    Date date = null;
    try {
      date = limit_end_date.getValue().getDate();
    } catch (Exception e) {
    }
    if (date == null) {
      return "";
    }
    return ScheduleUtils.translateDate(date, "yyyyMMdd");
  }

  /**
   * 期限フラグを取得します。
   * 
   * @return
   */
  public ALCellStringField getLimitFlag() {
    return limit_flag;
  }

  /**
   * 毎月繰り返す日を取得します。
   * 
   * @return
   */
  public ALCellNumberField getMonthDay() {
    return month_day;
  }

  /**
   * 繰り返しタイプを取得します。
   * 
   * @return
   */
  public ALCellStringField getRepeatType() {
    return repeat_type;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek0() {
    return week_0;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek1() {
    return week_1;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek2() {
    return week_2;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek3() {
    return week_3;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek4() {
    return week_4;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek5() {
    return week_5;
  }

  /**
   * 繰り返し曜日を取得します。
   * 
   * @return
   */
  public ALCellStringField getWeek6() {
    return week_6;
  }

  public ALCellDateTimeField getViewDate() {
    return view_date;
  }

  public int getInt(long num) {
    return (int) num;
  }

  /**
   * グループメンバーを取得します。
   * 
   * @return
   */
  public List getMemberList() {
    return memberList;
  }

  /**
   * 共有メンバーによる編集／削除権限フラグ
   * 
   * @return
   */
  public ALCellStringField getEditFlag() {
    return edit_flag;
  }

  /**
   * 繰り返しスケジュールの編集フラグ
   * 
   * @return
   */
  public ALCellNumberField getEditRepeatFlag() {
    return edit_repeat_flag;
  }

  public String getEntityId() {
    return entityid;
  }

  /**
   * 
   * @return
   */
  public boolean isMember() {
    return (is_member || memberList.size() > 1);
  }

  public ALCellStringField getChangeTmpreserveFlag() {
    return change_tmpreserve_flag;
  }

  public List getFacilityMemberList() {
    return facilityMemberList;
  }

  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }
}
