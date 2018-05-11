package com.xinchang.common.web;

public final class ExceptionConstants {
	private ExceptionConstants() {
	}

	public static final ResultEnums USERS_NOT_LOGIN = ResultEnums._100000;
	public static final ResultEnums USERS_LOGIN_USERNAME_OR_PASSWORD_ERROR = ResultEnums._100001;
	public static final ResultEnums USERS_UPDATE_USER_NOT_EXIST = ResultEnums._100002;
	public static final ResultEnums USERS_REG_MOBILE_PHONE_CANT_NULL = ResultEnums._100003;
	public static final ResultEnums USERS_MOBILE_PASSWORD_CANT_NULL = ResultEnums._100004;
	public static final ResultEnums USERS_QUERY_USER_NOT_EXISTS = ResultEnums._100006;
	public static final ResultEnums USERS_USERID_IS_NULL = ResultEnums._100007;
	public static final ResultEnums USERS_TOKEN_CACHE_IS_NULL = ResultEnums._100008;
	
	   
	public static final ResultEnums USERS_LOGIN_PARA_ERROR = ResultEnums._100009;
	
	public static final ResultEnums SEND_SMS_EXCEPTION = ResultEnums._100011;

	public static final ResultEnums MOBILE_PHONE_NUM_FORMAT_ERROR  = ResultEnums._100010;
	
	public static final ResultEnums USERS_LOGIN_AT_OTHER_TERMINAL  = ResultEnums._100015;
	
	public static final ResultEnums USER_NOT_EXSIT  = ResultEnums._100016;
	
	 public static final ResultEnums USERS_VERIFY_CODE_ERROR                            = ResultEnums._100012;
	  public static final ResultEnums USERS_VERIFY_CODE_TIMEOUT                          = ResultEnums._100013;
	 public static final ResultEnums USERS_VERIFY_CODE_ERROR_TIMES_MAX                  = ResultEnums._100014;
	    
	
	public static final ResultEnums HOUSE_ALREADY_LOCK = ResultEnums._200001;
	public static final ResultEnums HOUSE_LOCKIING = ResultEnums._200002;
	
	public static final ResultEnums HOUSE_ALREADY_RESERVER = ResultEnums._200003;
	public static final ResultEnums HOUSE_RESERVEING = ResultEnums._200004;
	
	public static final ResultEnums FAVORITE_EXCEED = ResultEnums._200005;
	
	public static final ResultEnums FAVORITE_DUPLICATE = ResultEnums._200007;
	
	public static final ResultEnums TEST_DATE_ALREADY_PASS = ResultEnums._200010;
	
	public static final ResultEnums OPEN_DATE_ALREADY_PASS = ResultEnums._200011;
	
	public static final ResultEnums EXP_DATE_ALREADY_PASS = ResultEnums._200012;
	
	public static final ResultEnums DATABASE_ERROR = ResultEnums._200006;
	
	public static final ResultEnums USER_UNIQUE_ORDER = ResultEnums._200008;
	
	public static final ResultEnums NOW_LESS_THEN_OPEN_TIME = ResultEnums._200009;
	
	public static final ResultEnums SYSTEM_CHANNEL_CODE_ILLEGAL = ResultEnums._999975;
	public static final ResultEnums SYSTEM_CLIENT_NEED_UPDATE = ResultEnums._999976;
	public static final ResultEnums SYSTEM_MISS_REQUIRED_HTTP_HEADER = ResultEnums._999977;
	public static final ResultEnums SYSTEM_ILLEGAL_REQUEST_PARAMETERS = ResultEnums._999978;
	public static final ResultEnums SYSTEM_MISSING_REQUEST_PARAMETERS = ResultEnums._999979;
	public static final ResultEnums SYSTEM_RETURN_OBJECT_TYPE_ERROR = ResultEnums._999980;
	public static final ResultEnums SYSTEM_CALL_REMOTE_SERVICE_TIME_OUT = ResultEnums._999981;
	public static final ResultEnums SYSTEM_REMOTE_SERVICE_NOT_FOUND = ResultEnums._999982;
	public static final ResultEnums SYSTEM_NO_SUCH_REQUEST_HANDLING_METHOD_EXCEPTION = ResultEnums._999983;
	public static final ResultEnums SYSTEM_HTTP_REQUEST_METHOD_NOT_SUPPORTED_EXCEPTION = ResultEnums._999984;
	public static final ResultEnums SYSTEM_HTTP_MEDIATYPE_NOT_SUPPORTED_EXCEPTION = ResultEnums._999985;
	public static final ResultEnums SYSTEM_HTTP_MEDIATYPE_NOT_ACCEPTABLE_EXCEPTION = ResultEnums._999986;
	public static final ResultEnums SYSTEM_MISSING_PATHVARIABLE_EXCEPTION = ResultEnums._999987;
	public static final ResultEnums SYSTEM_MISSING_SERVLET_REQUEST_PARAMETER_EXCEPTION = ResultEnums._999988;
	public static final ResultEnums SYSTEM_SERVLET_REQUEST_BINDING_EXCEPTION = ResultEnums._999989;
	public static final ResultEnums SYSTEM_CONVERSION_NOT_SUPPORTED_EXCEPTION = ResultEnums._999990;
	public static final ResultEnums SYSTEM_TYPE_MISMATCH_EXCEPTION = ResultEnums._999991;
	public static final ResultEnums SYSTEM_HTTP_MESSAGE_NOT_READABLE_EXCEPTION = ResultEnums._999992;
	public static final ResultEnums SYSTEM_HTTP_MESSAGE_NOT_WRITABLE_EXCEPTION = ResultEnums._999993;
	public static final ResultEnums SYSTEM_METHOD_ARGUMENT_NOT_VALID_EXCEPTION = ResultEnums._999994;
	public static final ResultEnums SYSTEM_MISSING_SERVLET_REQUEST_PART_EXCEPTION = ResultEnums._999995;
	public static final ResultEnums SYSTEM_BIND_EXCEPTION = ResultEnums._999996;
	public static final ResultEnums SYSTEM_NO_HANDLER_FOUND_EXCEPTION = ResultEnums._999997;
	public static final ResultEnums SYSTEM_SQL_EXCEPTION = ResultEnums._999998;
	public static final ResultEnums SYSTEM_OTHER_EXCEPTION = ResultEnums._999999;

	public enum ResultEnums {
		   /* ===============================用户模块相关异常，10开头=======================*/
        _100000("未登录或登录超时，请重新登录"),
        _100001("登录用户名或密码错误"),
        _100002("要更新的用户信息不存在"),
        _100003("注册时用户手机号不能为空"),
        _100004("注册时密码不能为空"),
        _100005("注册时昵称、性别不能为空"),
        _100006("要查询的用户信息不存在"),
        _100007("用户ID不能空"),
        _100008("Token不能为空"),
        _100009("您输入的信息有误，请联系置业顾问进行修改."),
        _100010("手机号格式错误"),
        _100011("发送验证码错误"),
        _100012("验证码错误"),
        _100013("验证码错误，请重新输入"),
        _100014("输入超过指定错误次数"),
        _100015("用户已在其他设备登录"),
        _100016("此手机号码不存在此系统,请确认"),
        _200001("该房源被选择中，还有机会哦"),
        _200002("其他客户正在锁定此房源中"),
        _200003("此房已售出，请选择其他房源"),
        _200004("其他用户正在预定此房源中"),
        _200005("已超出收藏限制数，最多每人收藏5套"),
        _200006("数据库异常"),
        _200007("不能收藏相同房源"),
        _200008("你已经选房，不能再选"),
        _200009("还没到开盘时间，请稍等"),
        _200010("已过公测时间。"),
        _200011("已过开盘时间。"),
        _200012("已过体验时间。"),
        
        
        /* ===============================系统相关异常，99开头=========================*/
        _999975("非法的渠道参数。"),
        _999976("您的客户端需要升级之后才可以继续使用，请到AppStore上下载最新版本。"),
        _999977("缺少必填的http请求头或格式不正确"),
        _999978("非法的请求参数"),
        _999979("缺少必填参数"),
        _999980("接口的返回值不是BaseVO对象的子类，请联系接口开发人员处理"),
        _999981("远程服务调用超时"),
        _999982("远程服务未找到"),
        _999983("there is no handler method (\"action\" method) for a specific HTTP request"),
        _999984("request handler does not support the specific request method"),
        _999985(
                "client POSTs, PUTs, or PATCHes content of a type not supported by request handler"),
        _999986("the request handler cannot generate a response that is acceptable by the client"),
        _999987(
                "the URI template does not match the path variable name declared on the method parameter"),
        _999988("missing servlet request parameter"),
        _999989(
                "fatal binding exception, thrown when we want to treat binding exceptions as unrecoverable"),
        _999990("no suitable editor or converter can be found for a bean property"),
        _999991("type mismatch when trying to set a bean property"),
        _999992(
                "thrown by {@link HttpMessageConverter} implementations when the {@link HttpMessageConverter#read} method fails"),
        _999993(
                "thrown by {@link HttpMessageConverter} implementations when the {@link HttpMessageConverter#write} method fails"),
        _999994("validation on an argument annotated with {@code @Valid} fails"),
        _999995(
                "raised when the part of a \"multipart/form-data\" request identified by its name cannot be found"),
        _999996("thrown when binding errors are considered fatal"),
        _999997(
                "by default when the DispatcherServlet can't find a handler for a request it sends a 404 response. However if its property throwExceptionIfNoHandlerFound is set to {@code true} this exception is raised and may be handled with a configured HandlerExceptionResolver"),
        _999998("数据库异常"),
        _999999("其它系统异常");
        /* ===============================系统模块相关异常，99开头=======================*/
		private String error; // 错误信息

		ResultEnums(String error) {
			this.error = error;
		}

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}
	}

}
