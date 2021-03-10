package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.web.context.ComponentContext;
import org.geektimes.projects.user.web.request.AccountRequest;
import org.geektimes.projects.user.web.request.RegisterRequest;
import org.geektimes.projects.user.web.response.CommonResponse;
import org.geektimes.web.mvc.controller.RestController;
import org.geektimes.web.mvc.header.annotation.PaddingParam;
import org.geektimes.web.mvc.header.annotation.ResponsePage;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * 登录页
 */
@Path("/rest")
public class LoginRestController implements RestController {

    @Path("/login")
    @POST
    public CommonResponse<String> execute(@PaddingParam AccountRequest account) throws Throwable {
        String success = "fail";

        CommonResponse<String> stringCommonResponse = new CommonResponse<>();
        stringCommonResponse.setCode(0);
        stringCommonResponse.setMsg("用户名/密码不正确，请核实后重试");
        stringCommonResponse.setData(success);
        return  stringCommonResponse;
    }

    @Path("/doRegister")
    @POST
    @ResponsePage
    public String doRegister(@PaddingParam RegisterRequest account, HttpServletRequest request) throws Throwable {
        UserService userService = ComponentContext.getComponentContext().getComponent("bean/UserService");
        request.getServletContext().log(new JSONObject(account).toString());
        User userRequest = new User();
        userRequest.setName(account.getName());
        userRequest.setPassword(account.getPassword());
        userRequest.setPhoneNumber(account.getPhoneNumber());
        userRequest.setEmail(account.getEmail());
        userService.register(userRequest);
        User user = userService.queryUserByNameAndPassword(userRequest.getName(), account.getPassword());
        request.setAttribute("userName", account.getName());
        request.setAttribute("success", account.getName());
        request.setAttribute("statusName", user != null ? "成功" : "失败");
        return "register-success.jsp";
    }
}
