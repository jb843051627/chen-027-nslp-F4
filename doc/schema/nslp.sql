-- nslp 建筑施工与社会生活噪声污染防治监管 -- schema (chen-027)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_027

CREATE TABLE IF NOT EXISTS t_nslp_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '噪声投诉与超标处置办件的件次',
  point_no varchar(64) DEFAULT NULL COMMENT '点位码（立件四样之一，一处点位一条）',
  zone_no varchar(64) DEFAULT NULL COMMENT '所属区册代号（立件四样之一，以底册带出的为准）',
  occ_num int DEFAULT NULL COMMENT '处数（立件四样之一，至少一处）',
  check_reading decimal(12,1) DEFAULT NULL COMMENT '准备折算的那一个核查读数（立件四样之一，折算前先取一位小数逢半进一）',
  stage int DEFAULT NULL COMMENT '当前关次 0..3（受件/核查/责令/复查收尾，关次由后端数，报文自带的只作参考不入库）',
  status int DEFAULT NULL COMMENT '单子走到哪一关 0未起 1在办 2已封住',
  content varchar(255) DEFAULT NULL COMMENT '本关一句（同一关第二句盖不上去，本行只留头一句）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪关动作',
  order_accept varchar(16) DEFAULT NULL COMMENT '责令关核收原话；记成「可」复查收尾才开得了工',
  witness_a varchar(64) DEFAULT NULL COMMENT '复查收尾见证岗甲落笔（两名各落一笔）',
  witness_b varchar(64) DEFAULT NULL COMMENT '复查收尾见证岗乙落笔（两名各落一笔）',
  proof_no varchar(128) DEFAULT NULL COMMENT '复测凭证那一串（封关时写回本行）',
  note_mask int DEFAULT 0 COMMENT '本趟各关头一句按语位图 bit0..bit3（收回的关腾位，重新挪到从头累计）',
  close_kind int DEFAULT NULL COMMENT '收口情由 空=走满末关封住 1反映人撤回 2区册并走（后两种视同收口）',
  void_flag int DEFAULT 0 COMMENT '末关作废 0正常 1已作废（作废的件名册照常露脸，只是不给动手的把手）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_point_open (point_no, status, void_flag, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='噪声投诉与超标处置办件流转单';

-- 一条办件身后的留痕：每一关那句按语连同提交那一刻，按时序压在本行身后；
-- 改档与留痕同一事务，日后追经过只取这一串（档数由这一串放出来，不许另算）。
CREATE TABLE IF NOT EXISTS t_nslp_case_trail (
  id bigint NOT NULL COMMENT '主键',
  case_id bigint NOT NULL COMMENT '所随办件 t_nslp_case_flow.id',
  stage int DEFAULT NULL COMMENT '落笔那一刻办件所处关次 0..3',
  action varchar(32) DEFAULT NULL COMMENT '动作 受件立条/挪前/收回/按语/封住/反映人撤回/区册并走/作废',
  note varchar(500) DEFAULT NULL COMMENT '这一笔留下的话（按语原句或动作按语）',
  trail_time datetime DEFAULT NULL COMMENT '提交那一刻（同刻两笔各记各的先后，按本列排先后不许并成一条）',
  seq int DEFAULT NULL COMMENT '同刻先后次序（trail_time 并列时以本列断先后）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_case_trail (case_id, trail_time, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='办件流转留痕（一关一句，按时序压在身后）';

CREATE TABLE IF NOT EXISTS t_nslp_doc_tpl (
  id bigint NOT NULL COMMENT '主键',
  tpl_no varchar(64) DEFAULT NULL COMMENT '文书模板代号',
  tpl_name varchar(128) DEFAULT NULL COMMENT '文种名（超标告知·责令限期改正·复查结论）',
  ver_no int DEFAULT NULL COMMENT '版次（数值大的先说话）',
  var_num int DEFAULT NULL COMMENT '本版应填项数',
  fill_num int DEFAULT NULL COMMENT '本行已取到值的项数',
  miss_num int DEFAULT NULL COMMENT '还差几项（轧出来，不手填）',
  eff_start datetime DEFAULT NULL COMMENT '起用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  status int DEFAULT NULL COMMENT '版次情形 0在位 1已撤版',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执法文书模板与版次';

-- 出了文的每一纸都在档上留一份快照：出文那一刻照的是哪一版，就把那版的
-- 版次、应填/已取项数连同样张行一起钉在档上。往后新立一版只供新文，
-- 绝不回改旧档（前年新版措辞回灌旧文那类事，一道堵死）。
CREATE TABLE IF NOT EXISTS t_nslp_doc_issue (
  id bigint NOT NULL COMMENT '主键',
  point_id bigint DEFAULT NULL COMMENT '照哪一行点位出的文 t_nslp_point.id',
  point_no varchar(64) DEFAULT NULL COMMENT '出文时点位码（冗余自点位行）',
  tpl_id bigint DEFAULT NULL COMMENT '出文那一刻照的那一版主键 t_nslp_doc_tpl.id',
  tpl_no varchar(64) DEFAULT NULL COMMENT '文书模板代号（快照）',
  tpl_name varchar(128) DEFAULT NULL COMMENT '文种名（快照：超标告知·责令限期改正·复查结论）',
  ver_no int DEFAULT NULL COMMENT '版次（出文那一刻的开口版，钉死）',
  var_num int DEFAULT NULL COMMENT '本版应填项数（快照）',
  fill_num int DEFAULT NULL COMMENT '已取到值的项数（快照）',
  miss_num int DEFAULT NULL COMMENT '还差几项（快照：var-fill）',
  issued_at datetime DEFAULT NULL COMMENT '出文那一刻（选版就看这一刻在不在线）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者（谁出的文）',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_issue_point (point_id),
  KEY idx_issue_tpl (tpl_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执法文书出文档案（一纸一版，钉住出文那一刻的版次快照）';

CREATE TABLE IF NOT EXISTS t_nslp_flow_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '监测流水批次码（一拨一码；同一码第二回到达只照面，不另起一拨）',
  cap_num int DEFAULT NULL COMMENT '写定的顶（抬头那一格空着/填负/填得离谱，一律按300认后落库，只此一处来源）',
  row_num int DEFAULT NULL COMMENT '进门点出来的行数（事后翻得出来当初点了几行）',
  parse_kind varchar(16) DEFAULT NULL COMMENT '本拨切法 DELIMITER按分隔符／FIXED按定宽（一拨之内只许一种）',
  status int DEFAULT NULL COMMENT '拨落地情形 0已核收 1超顶整批未收',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测流水核收拨抬头';

CREATE TABLE IF NOT EXISTS t_nslp_flow_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '监测流水批次码',
  row_no int DEFAULT NULL COMMENT '原报行次（系统在本拨内排，不留手填格子；退回重报仍显示当初那一个行次）',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的点位码',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行读数次数',
  chk_num decimal(12,2) DEFAULT NULL COMMENT '本行达标次数',
  put_kind varchar(32) DEFAULT NULL COMMENT '来路（自动站直采／监测车补报两条来路）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回（第四种写法不许落库）',
  ret_reason varchar(255) DEFAULT NULL COMMENT '退回理由（吃了哪一条，一句一句问得出）；未退空白',
  parse_flag int DEFAULT '0' COMMENT '解析情形 0切得动 1切不动（单独标出递给回核看的人，不按住整拨）',
  reg_diff_flag int DEFAULT '0' COMMENT '底册比对 0相符或未报 1不合（以底册为准，回执单列）',
  point_name varchar(128) DEFAULT NULL COMMENT '点位题名（以点位底册带出的为准，报文那串只拿来比对）',
  zone_no varchar(64) DEFAULT NULL COMMENT '区册代号（以点位底册带出的为准）',
  layer_flag int DEFAULT '0' COMMENT '册层 0现行 1历史（未勾连满两个年度转入历史册层，只此一处挪动）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测流水核收行';

CREATE TABLE IF NOT EXISTS t_nslp_limit_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '功能区限值线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  obj_kind varchar(32) DEFAULT NULL COMMENT '这条线管昼段还是夜段',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算限值',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线限值',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶限值',
  eff_start datetime DEFAULT NULL COMMENT '起用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能区昼夜间限值线';

CREATE TABLE IF NOT EXISTS t_nslp_permit_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '夜间连续施工作业许可签批单',
  node_no int DEFAULT NULL COMMENT '当前所在关 0..2（窗口受理/区局核定/市局备案）',
  sign_mode int DEFAULT NULL COMMENT '本关算齐办法 0一名即过 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本关应签人数',
  sign_count int DEFAULT NULL COMMENT '本关已到人数',
  status int DEFAULT NULL COMMENT '签批情形 0在签 1已点齐 2已压回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='夜间连续施工作业许可签批单';

CREATE TABLE IF NOT EXISTS t_nslp_point (
  id bigint NOT NULL COMMENT '主键',
  point_no varchar(64) DEFAULT NULL COMMENT '点位码（随报送材料进来，系统不另发第二串，也不核对构成规矩）',
  annual int DEFAULT NULL COMMENT '年度（同一年度同一区册底下去重点位码；旧纸簿搬进来的留空）',
  zone_id int DEFAULT NULL COMMENT '挂在哪一个区册名下',
  zone_no varchar(64) DEFAULT NULL COMMENT '区册代号（冗余自区册，以区册底为准改齐）',
  zone_name varchar(128) DEFAULT NULL COMMENT '区册全称（冗余自区册；对不齐时以区册为准把本行改过来）',
  src_kind varchar(128) DEFAULT NULL COMMENT '来路（建档岗一条一条建／复测业务核收后带出，两边同一套算法同一个样子）',
  need_num int DEFAULT NULL COMMENT '核定应测次数',
  got_num int DEFAULT NULL COMMENT '已核验达标的次数',
  lack_num int DEFAULT NULL COMMENT '还差几回（轧出来，不手填：need-got，每回写库之前重轧）',
  content varchar(255) DEFAULT NULL COMMENT '随件交来的点位题名与依附声源',
  status int DEFAULT NULL COMMENT '册面情形 0新入册 1已核齐 2缺项',
  walk_status int DEFAULT NULL COMMENT '在走三态 0待测 1已测 2压卡（旧纸簿搬进来的留空，不算压卡）',
  halted int DEFAULT 0 COMMENT '就地叫停 0在走 1已叫停（叫停腾出同点位名额；压卡也腾名额）',
  pinned_rule_code varchar(64) DEFAULT NULL COMMENT '归层钉住的限值线代号（不归层清空白，追得出当年照哪条线）',
  pinned_eff_start datetime DEFAULT NULL COMMENT '归层钉住的起用那一一日（不归层清空白）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者（谁建的册，复核点得出落笔人）',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者（谁改的行，复核点得出落笔人）',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_point_open (point_no, annual, zone_no, walk_status, halted, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声环境监测点位底册';

-- 点位在走留痕：待测→已测→压卡的每一步按时序压在点位行身后。
-- 收回重来时先前那一趟的记录照旧留着，不抹、不跟后补的并排；
-- 这一行眼下走到哪一步，由系统依这一串已走步数自行数出，屏上写的、人记的都不作依据。
CREATE TABLE IF NOT EXISTS t_nslp_point_trail (
  id bigint NOT NULL COMMENT '主键',
  point_id bigint NOT NULL COMMENT '所随点位 t_nslp_point.id',
  step int DEFAULT NULL COMMENT '落笔那一刻所处步数 0待测 1已测 2压卡',
  action varchar(32) DEFAULT NULL COMMENT '动作 建行/推进/收回/叫停/压卡',
  trail_time datetime DEFAULT NULL COMMENT '提交那一刻（同刻两笔各记各的先后）',
  seq int DEFAULT NULL COMMENT '同刻先后次序（trail_time 并列时以本列断先后）',
  note varchar(500) DEFAULT NULL COMMENT '这一笔留下的话',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_point_trail (point_id, trail_time, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点位在走留痕（步由这一串放出来，不许另算）';

CREATE TABLE IF NOT EXISTS t_nslp_task_item (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '复测与巡查条目代号',
  due_at datetime DEFAULT NULL COMMENT '该发复测通知那一一日（落库为 datetime）',
  amount decimal(12,2) DEFAULT NULL COMMENT '最多许提前几日',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对点位的记要',
  status int DEFAULT NULL COMMENT '条目情形 0搁着 1已发出 2发不成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复测与巡查条目';

CREATE TABLE IF NOT EXISTS t_nslp_zone_book (
  id bigint NOT NULL COMMENT '主键',
  zone_no varchar(64) DEFAULT NULL COMMENT '声环境功能区区册代号',
  zone_name varchar(128) DEFAULT NULL COMMENT '区册全称',
  zone_kind varchar(32) DEFAULT NULL COMMENT '区层类别（甲层住宅·乙层混合·丙层工业·丁层交通干线）',
  road_name varchar(128) DEFAULT NULL COMMENT '属地详址（澜原省—江水市—区—片区）',
  align_kind varchar(32) DEFAULT NULL COMMENT '来路（市里区册下发／本局自录）',
  status int DEFAULT NULL COMMENT '区册情形 0在册 1已撤出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声环境功能区区册底册';

-- 初始档案数据（t_nslp_zone_book 预置两条区册种子）
-- zone_id=0 为在册区册（甲层住宅类），zone_id=1 为已并走撤出的旧区册（乙层），供挂靠校验与挡新入册校验取用。
-- 区册代号与区层类别按区层序给值。
INSERT IGNORE INTO t_nslp_zone_book (id, zone_no, zone_name, zone_kind, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'QN00', '澜原省江水市云桥区临江片区（甲层住宅类，在册）', '甲层', '澜原省—江水市—云桥区—临江片区', 0, 0, 'seed', NOW()),
       (1, 'QN01', '南津区北岸片区·并走撤出的旧区册（已撤出）', '乙层', '澜原省—江水市—南津区—北岸片区', 1, 0, 'seed', NOW());

