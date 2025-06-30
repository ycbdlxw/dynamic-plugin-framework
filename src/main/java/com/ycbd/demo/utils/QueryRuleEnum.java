package com.ycbd.demo.utils;

public enum QueryRuleEnum {

    GT(">", "gt", "大于"),
    GE(">=", "gte", "大于等于"),
    LT("<", "lt", "小于"),
    LE("<=", "lte", "小于等于"),
    EQ("=", "eq", "等于"),
    NE("!=", "ne", "不等于"),
    IN(" IN ", "in", "包含"),
    LEFTIN(" LEFTIN ", "leftin", "左边IN"),
    LIKE(" LIKE ", "like", "全模糊"),
    LIKEBINARY(" LIKE BINARY ", "like binary", "特殊字符查找"),
    LEFT_LIKE(" LEFT_LIKE ", "left_like", "左模糊"),
    RIGHT_LIKE(" RIGHT_LIKE ", "right_like", "右模糊"),
    FIND_IN_SET(" FIND_IN_SET ", "find_in_set", "批量查询"),
    REGEXP(" REGEXP ", "regexp", "正则表达式"),
    RANGE(" RANGE ", "RANGE", "范围"),
    SQL_RULES("USE_SQL_RULES", "ext", "自定义SQL片段");

    private String value;

    private String condition;

    private String msg;

    QueryRuleEnum(String value, String condition, String msg) {
        this.value = value;
        this.condition = condition;
        this.msg = msg;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public static QueryRuleEnum getByValue(String value) {

        for (QueryRuleEnum val : values()) {
            if (val.getValue().equals(value) || val.getCondition().equalsIgnoreCase(value.trim())) {
                return val;
            }
        }
        return null;
    }

    public static QueryRuleEnum getByMsg(String value) {
        for (QueryRuleEnum val : values()) {
            if (val.getMsg().equals(value) || val.getMsg().equalsIgnoreCase(value.trim())) {
                return val;
            }
        }
        return null;
    }
}
