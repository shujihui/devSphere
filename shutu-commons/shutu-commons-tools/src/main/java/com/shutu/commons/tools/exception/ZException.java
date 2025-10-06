package com.shutu.commons.tools.exception;

import com.shutu.commons.tools.utils.MessageUtils;
import com.shutu.commons.tools.exception.ErrorCode;
import com.shutu.commons.tools.utils.MessageUtils;

/**
 * 自定义异常
 */
public class ZException extends RuntimeException {
	private static final long serialVersionUID = 1L;

    private int code;
	private String msg;

	public ZException(int code) {
		this.code = code;
		this.msg = MessageUtils.getMessage(code);
	}

	public ZException(int code, String... params) {
		this.code = code;
		this.msg = MessageUtils.getMessage(code, params);
	}

	public ZException(int code, Throwable e) {
		super(e);
		this.code = code;
		this.msg = MessageUtils.getMessage(code);
	}

	public ZException(int code, Throwable e, String... params) {
		super(e);
		this.code = code;
		this.msg = MessageUtils.getMessage(code, params);
	}

	public ZException(String msg) {
		super(msg);
		this.code = ErrorCode.INTERNAL_SERVER_ERROR;
		this.msg = msg;
	}

	public ZException(String msg, Throwable e) {
		super(msg, e);
		this.code = ErrorCode.INTERNAL_SERVER_ERROR;
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

}