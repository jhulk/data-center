package com.yjq.data.admin.web.advice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yjq.data.admin.common.ErrorEnum;
import com.yjq.data.admin.common.exception.ParamInvalidException;
import com.yjq.data.admin.common.exception.ServiceException;
import com.yjq.data.admin.common.exception.SessionInvalidException;
import com.yjq.data.admin.common.validation.MyValidator;
import com.yjq.data.admin.model.dto.response.ResponseDTO;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.yjq.data.client.api.core.DataQueryErrorEnum;
import com.yjq.data.client.api.core.DataQueryException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * 统一异常处理
 * @date 2017/8/16
 * @author netyjq@gmail.com
 */
@ControllerAdvice
public class ExceptionConfig {

    private static Logger logger = LoggerFactory.getLogger(ExceptionConfig.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    HttpServletResponse response;

    /**
     * 业务异常
     * @param e ServiceException
     */
    @ExceptionHandler(ServiceException.class)
    public void serviceException(ServiceException e) {
        writeToClient(ErrorEnum.BIZ_ERROR.buildMessage(e.getMessage()), e);
    }

    /**
     * SQL异常
     * @param e SQLException
     */
    @ExceptionHandler(SQLException.class)
    public void sqlException(SQLException e) {
        writeToClient(ErrorEnum.DB_EXECUTE_ERROR, e);
    }

    @ExceptionHandler(MySQLIntegrityConstraintViolationException.class)
    public void uniqueException(MySQLIntegrityConstraintViolationException e) {
        writeToClient(ErrorEnum.DB_UNIQUE_ERROR, e);
    }

    /**
     * mybatis异常拦截
     * @param e
     */
    @ExceptionHandler(PersistenceException.class)
    public void persistenceException(PersistenceException e) {
        writeToClient(ErrorEnum.DB_EXECUTE_ERROR, e);
    }

    /**
     * 参数业务
     * @param e ParamValidException
     */
    @ExceptionHandler(ParamInvalidException.class)
    public void paramInvalidException(ParamInvalidException e) {
        writeToClient(ErrorEnum.WEB_PARAM_ERROR.buildMessage(e.getMessage()), e);
    }

    @ExceptionHandler(SessionInvalidException.class)
    public void sessionInvalidException(SessionInvalidException e) {
        writeToClient(ErrorEnum.SESSION_LOST_ERROR.buildMessage(e.getMessage()), e);
    }

    @ExceptionHandler(DataQueryException.class)
    public void dataQueryException(DataQueryException e) {
        writeToClient(ErrorEnum.RPC_ERROR.buildMessage(e.getMessage()), e);
    }


    /**
     * 绑定异常
     * @param e
     */
    @ExceptionHandler(BindException.class)
    public void bindException(BindException e) {
        BindingResult result = e.getBindingResult();
        if (result !=null && result.hasErrors()) {
            MyValidator.validateBindResult(result);
        }
    }

    @ExceptionHandler(NullPointerException.class)
    public void nullPointerException(NullPointerException e) {
        writeToClient(ErrorEnum.UNKNOWN_ERROR, e);
    }

    private void writeToClient(ErrorEnum errorEnum, Exception e) {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setResult(errorEnum.getCode());
        responseDTO.setMessage(errorEnum.getMsg());
        responseDTO.setType(errorEnum.name());
        write(JSON.toJSONString(responseDTO), e);
    }

    private void write(String message, Exception e) {
        PrintWriter pw = null;
        try {
            response.setContentType("application/json;charset=UTF-8" );
            pw = response.getWriter();
            pw.write(message);
            pw.flush();
        } catch (Exception ex) {
        } finally {
            if (pw != null) {
                pw.close();
            }
            logger.error("【统一异常】异常详情: {}, 参数: {}", message, getRequestParams(request), e);
        }
    }

    /**
     * 获取请求的基本参数
     * @param request
     * @return
     */
    private String getRequestParams(HttpServletRequest request) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("requestUrl", request.getRequestURL());
        jsonObject.put("method", request.getMethod());
        jsonObject.put("parameter", request.getParameterMap());
        return jsonObject.toJSONString();
    }
}
