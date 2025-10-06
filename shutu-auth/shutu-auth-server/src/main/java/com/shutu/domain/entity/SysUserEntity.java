package com.shutu.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.poi.hpsf.Decimal;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户管理
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 真实姓名
	 */
	private String realName;
	/**
	 * 头像
	 */
	private String headUrl;
	/**
	 * 性别   0：男   1：女    2：保密
	 */
	private Integer gender;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 默认收获地址
	 */
	private Long addressId;
	/**
	 * 店铺id
	 */
	private Long shopId;
	/**
	 * 用户余额
	 */
	private BigDecimal balance;
	/**
	 * 身份证号
	 */
	private String idCard;
	/**
	 * 手机号
	 */
	private String mobile;
	/**
	 * 超级管理员   0：否   1：是
	 */
	private Integer superAdmin;
	/**
	 * 状态  0：停用    1：正常
	 */
	private Integer status;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 部门ID
	 */
	private Long deptId;
	/**
	 * 删除标识  0：未删除    1：删除
	 */
	@TableField(fill = FieldFill.INSERT)
	private Integer delFlag;
	/**
	 * 更新者
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Long updater;
	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateDate;

	/**
	 * 部门名称
	 */
	@TableField(exist = false)
	private String deptName;

}
