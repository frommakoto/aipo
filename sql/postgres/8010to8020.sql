--
-- Aipo is a groupware program developed by TOWN, Inc.
-- Copyright (C) 2004-2015 TOWN, Inc.
-- http://www.aipo.com
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.
--

-- 20150609
ALTER TABLE EIP_T_TIMELINE_LIKE DROP CONSTRAINT eip_t_timeline_like_timeline_like_id_fkey;
DELETE FROM EIP_T_TIMELINE_LIKE WHERE TIMELINE_ID NOT IN( SELECT TIMELINE_ID FROM EIP_T_TIMELINE );
ALTER TABLE EIP_T_TIMELINE_LIKE ADD FOREIGN KEY (TIMELINE_ID) REFERENCES EIP_T_TIMELINE (TIMELINE_ID) ON DELETE CASCADE;
-- 20150609

-- 20151109
UPDATE EIP_T_ACL_PORTLET_FEATURE SET ACL_TYPE = 19 WHERE FEATURE_NAME = 'workflow_request_other' AND FEATURE_ALIAS_NAME = 'ワークフロー（他ユーザーの依頼）操作';
UPDATE EIP_T_ACL_ROLE SET ACL_TYPE = 19 WHERE  FEATURE_ID IN (SELECT FEATURE_ID FROM EIP_T_ACL_PORTLET_FEATURE WHERE FEATURE_NAME = 'workflow_request_other') AND ROLE_NAME = 'ワークフロー（他ユーザーの依頼）管理者';
-- 20151109
