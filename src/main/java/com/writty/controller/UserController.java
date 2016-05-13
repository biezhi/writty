package com.writty.controller;

import java.io.File;
import java.util.Date;

import com.blade.Blade;
import com.blade.ioc.annotation.Inject;
import com.blade.route.annotation.Path;
import com.blade.route.annotation.Route;
import com.blade.view.ModelAndView;
import com.blade.web.http.HttpMethod;
import com.blade.web.http.Request;
import com.blade.web.http.Response;
import com.blade.web.multipart.FileItem;
import com.writty.Constant;
import com.writty.kit.SessionKit;
import com.writty.kit.Utils;
import com.writty.model.User;
import com.writty.service.PostService;
import com.writty.service.UserService;

import blade.kit.DateKit;
import blade.kit.EncrypKit;
import blade.kit.FileKit;
import blade.kit.PatternKit;
import blade.kit.StringKit;
import blade.kit.json.JSONObject;

@Path("/")
public class UserController extends BaseController {
	
	@Inject
	private UserService userService;
	
	@Inject
	private PostService postService;
	
	/**
	 * 个人设置页面
	 */
	@Route(value = "/settings", method = HttpMethod.GET)
	public ModelAndView show_settings(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		return this.getView("settings");
	}
	
	/**
	 * 申请编辑页面
	 */
	@Route(value = "/request", method = HttpMethod.GET)
	public ModelAndView show_request(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		return this.getView("request");
	}
	
	/**
	 * 申请编辑
	 */
	@Route(value = "/request", method = HttpMethod.POST)
	public void requestEditor(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}
		
		// github帐号
		String github = request.query("github");
		// 联系邮箱
		String email = request.query("email");
		// 个人介绍
		String intro = request.query("intro");
		
		if(StringKit.isBlank(github) || StringKit.isBlank(email) ||
				StringKit.isBlank(intro)){
			this.error(response, "参数不能为空");
			return;
		}
		
		if(!Utils.isEmail(email)){
			this.error(response, "邮箱格式错误");
			return;
		}
		
		if(intro.length() < 50){
			this.error(response, "请完善您的个人介绍");
			return;
		}
		
	}
	
	/**
	 * 修改密码
	 */
	@Route(value = "/reset_pwd", method = HttpMethod.GET)
	public void reset_pwd(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return;
		}
		String pwd = request.query("pwd");
		if(StringKit.isBlank(pwd) || pwd.length() < 6 || pwd.length() > 20){
			this.error(response, "请输入6 - 20位密码");
			return;
		}
		String newpwd = EncrypKit.md5(user.getUser_name() + pwd);
		userService.updatePwd(user.getUid(), newpwd);
	}
	
	/**
	 * 写文章页面
	 */
	@Route(value = "/write", method = HttpMethod.GET)
	public ModelAndView show_write(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			response.go("/");
			return null;
		}
		return this.getView("write");
	}
	
	/**
	 * 保存文章
	 */
	@Route(value = "/write", method = HttpMethod.POST)
	public void save_write(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			this.nosignin(response);
			return;
		}
		
		String title = request.query("title");
		String cover = request.query("cover");
		String content = request.query("content");
		Long sid = request.queryAsLong("sid");
		Integer is_pub = request.queryAsInt("is_pub");
		
		if(StringKit.isBlank(title) || 
				StringKit.isBlank(cover) ||
				StringKit.isBlank(content) ||
				null == sid || null == is_pub){
			this.error(response, "参数不能为空");
			return;
		}
		
		if(title.length() < 5 || title.length() > 20){
			this.error(response, "请输入5-20个字符长度的标题");
			return;
		}
		
		if(content.length() < 100){
			this.error(response, "请输入100字以上的文章内容");
			return;
		}
		
		boolean flag = postService.save(title, null, user.getUid(), sid, is_pub, cover, content);
		if(flag){
			this.success(response, "");
		} else {
			this.error(response, "文章发布失败");
		}
	}
	
	/**
	 * 上传头像
	 */
	@Route(value = "/uploadimg", method = HttpMethod.POST)
	public void uploadimg(Request request, Response response){
		User user = SessionKit.getLoginUser();
		if(null == user){
			return;
		}
		
		FileItem[] fileItems = request.files();
		if(null != fileItems && fileItems.length > 0){
			
			FileItem fileItem = fileItems[0];
			
			String type = request.query("type");
			String suffix = FileKit.getExtension(fileItem.getFileName());
			if(StringKit.isNotBlank(suffix)){
				suffix = "." + suffix;
			}
			if(!PatternKit.isImage(suffix)){
				return;
			}
			
			if(null == type){
				type = "temp";
			}
			
			String saveName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10) + suffix;
			File file = new File(Blade.me().webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
			
			try {
				
				Utils.copyFileUsingFileChannels(fileItem.getFile(), file);
				
				String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
				
				JSONObject res = new JSONObject();
				res.put("status", 200);
				res.put("savekey", filePath);
				res.put("savepath", filePath);
				res.put("url", Constant.SITE_URL + "/" + filePath);
				
				response.json(res.toString());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
