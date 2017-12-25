package com.zt.upload.controller;

import com.zt.upload.fileupload.UploadStatus;
import com.zt.upload.utils.FileUtils;
import com.zt.upload.utils.module.UploadFile;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;

/**
 * @author zhangtong
 * Created by on 2017/12/25
 */
@Controller
@RequestMapping("cont")
public class Cont {

    @RequestMapping("index")
    public String index() {
        return "index";
    }

    /**
     * 保存 --上报文章信息
     *
     * @return
     */
    @RequestMapping(value = "saveReportArticleFile", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
    @ResponseBody
    public Object saveReportArticleFile(HttpServletRequest request) {
        String reportCode = request.getParameter("reportCode");
        String filePath = System.getProperty("user.dir") + File.separator + "reportFile" + File.separator + reportCode + File.separator;
        FileUtils fileUtils = new FileUtils();
        List<UploadFile> files = fileUtils.getFiles(request, filePath);
        JSONObject result = new JSONObject();
        result.put("state", true);
        return result;
    }

    /**
     * 这里是获取上传文件状态信息的访问接口
     *
     * @param session
     * @return
     */
    @ResponseBody
    @RequestMapping("getStatus")
    public UploadStatus getStatus(HttpServletRequest request) {
        HttpSession session = request.getSession();
        System.out.println("widddd：" + (UploadStatus) session.getAttribute("upload_status"));
        return (UploadStatus) session.getAttribute("upload_status");
    }

}