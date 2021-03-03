package org.geektimes.projects.user.web.response;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @Author: menglinggang
 * @Date: 2021-03-03
 * @Time: 9:05 上午
 */
public class CommonResponse<T> {

    private int code;
    private String msg;
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static CommonResponse failResponse() {
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCode(-100);
        return  commonResponse;
    }


    public static CommonResponse successResponse() {
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCode(0);
        return commonResponse;
    }
}
